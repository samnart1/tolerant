package processor

import (
	"errors"
	"fmt"
	"log"
	"math/rand"
	"time"

	"github.com/samnart/thesis/internal/resilience"
	"github.com/samnart/thesis/pkg/models"
)

type PaymentProcessor struct {
	circuitBreaker *resilience.CircuitBreaker
}

func NewPaymentProcessor() *PaymentProcessor {
	return &PaymentProcessor{
		circuitBreaker: resilience.NewCircuitBreaker(5, 10, 30*time.Second),
	}
}

func (pp *PaymentProcessor) ProcessPayment(request *models.PaymentRequest) error {
	log.Printf("procesing payment for order %s: €%.2f via %s", request.OrderID, request.Amount, request.PaymentMethod)

	//execute with circuit breaker
	err := pp.circuitBreaker.Execute(func() error {
		return pp.processPaymentInternal(request)
	})

	if err != nil {
		return fmt.Errorf("payment processing failed: %w", err)
	}

	return  nil
}

func (pp *PaymentProcessor) processPaymentInternal(request *models.PaymentRequest) error {
	//simulate processing time
	processingTime := time.Duration(50+rand.Intn(150)) * time.Millisecond
	time.Sleep(processingTime)

	if rand.Float32() < 0.1 {
		return errors.New("payment gateway timeout")
	}

	if request.Amount <= 0 {
		return errors.New("invalid payment amount")
	}

	if request.PaymentMethod == "" {
		return errors.New("payment method required")
	}

	switch request.PaymentMethod {
	case "credit_card":
		return pp.processCreditCard(request)
	case "debit_card":
		return pp.processDebitCard(request)
	case "pay_pal":
		return pp.processPayPal(request)
	default:
		return fmt.Errorf("unsupported method: %s", request.PaymentMethod)
	}
}

func (pp *PaymentProcessor) processCreditCard(request *models.PaymentRequest) error {
	log.Printf("processing credit card payment for order %s", request.OrderID)

	time.Sleep(100*time.Millisecond)

	if rand.Float32() < 0.05 {
		return errors.New("credit card declines")
	}

	log.Printf("credit card payment successful for order %s", request.OrderID)
	return nil
}

func (pp *PaymentProcessor) processDebitCard(request *models.PaymentRequest) error {
	log.Printf("processing debit card payment for order %s", request.OrderID)

	time.Sleep(80 * time.Millisecond)

	if rand.Float32() < 0.003 {
		return errors.New("insufficient funds")
	}

	log.Printf("Debit card payment successful for order %s", request.OrderID)
	return nil
}

func (pp *PaymentProcessor) processPayPal(request *models.PaymentRequest) error {
	log.Printf("Processing PayPal payment for order %s", request.OrderID)

	//simulating paypal payment processing
	time.Sleep(150 * time.Millisecond)

	if rand.Float32() < 0.02 {
		return errors.New("paypal authorization failed")
	}

	log.Printf("Paypal payment successful for order %s", request.OrderID)
	return nil
}