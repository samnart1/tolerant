package consumer

import (
	"encoding/json"
	"log"
	"time"

	"github.com/samnart/thesis/internal/processor"
	"github.com/samnart/thesis/internal/resilience"
	"github.com/samnart/thesis/pkg/models"
	"github.com/streadway/amqp"
)

type PaymentConsumer struct {
	conn      *amqp.Connection
	channel   *amqp.Channel
	processor *processor.PaymentProcessor
	done      chan bool
	retrier   *resilience.RetryHandler
}

func (p *PaymentConsumer) Stop() {
	log.Println("stopping the payment consumer......")
	close(p.done)

	if p.channel != nil {
		p.channel.Close()
	}

	if p.conn != nil {
		p.conn.Close()
	}

	log.Println("payment consumer stopped")
}

func (p *PaymentConsumer) Start() error {
	msgs, err := p.channel.Consume(
		"payment_queue",
		"",
		false,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		return err
	}

	log.Println("payment consumer started, waiting for messages.......")

	go func() {
		for {
			select {
			case msg, ok := <-msgs:
				if !ok {
					log.Println("message channel closed")
					return 
				}
				p.handleMessage(msg)
			case <-p.done:
				log.Println("consumer stopping......")
				return 
			}
		}
	}()

	return  nil
}

func (p *PaymentConsumer) handleMessage(msg amqp.Delivery) {
	var paymentRequest models.PaymentRequest

	err := json.Unmarshal(msg.Body, &paymentRequest)
	if err != nil {
		log.Printf("failed to parse payment request: %v", err)
		msg.Nack(false, false) //invalid message
		return
	}

	log.Printf("received payment request for order: %s", paymentRequest.OrderID)

	//process with retry logic
	err = p.retrier.Execute(func() error {
		return p.processor.ProcessPayment(&paymentRequest)
	})

	if err != nil {
		log.Printf("failed to process payment for order %s after retries: %v", paymentRequest.OrderID, err)

		//check whether to requeue base on error type
		requeue := shouldRequeue(msg)
		msg.Nack(false, requeue)
		return
	}

	log.Printf("successfully processed payment for order: %s", paymentRequest.OrderID)
	msg.Ack(false)
}

func shouldRequeue(msg amqp.Delivery) bool {
	//if the message has been delivered severally, don't requeue
	if msg.Headers != nil {
		if xDeath, ok := msg.Headers["x-death"].([]interface{}); ok && len(xDeath) > 0 {
			if death, ok := xDeath[0].(amqp.Table); ok {
				if count, ok := death["count"].(int64); ok && count >= 3 {
					return false
				}
			}
		}
	}
	return true
}

func NewPaymentConsumer(rabbitMQURL string, processor *processor.PaymentProcessor) (*PaymentConsumer, error) {
	//connect with retry logic
	var conn *amqp.Connection
	var err error

	retryConfig := resilience.RetryConfig{
		MaxAttempts:       5,
		InitialDelay:      2 * time.Second,
		MaxDelay:          30 * time.Second,
		BackoffMultiplier: 2.0,
	}

	err = resilience.Retry(retryConfig, func() error {
		conn, err = amqp.Dial(rabbitMQURL)
		return err
	})

	if err != nil {
		return nil, err
	}

	channel, err := conn.Channel()
	if err != nil {
		conn.Close()
		return nil, err
	}

	_, err = channel.QueueDeclare(
		"payment_queue",
		true,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		channel.Close()
		conn.Close()
		return nil, err
	}

	err = channel.Qos(
		1,
		0,
		false,
	)
	if err != nil {
		channel.Close()
		conn.Close()
		return nil, err
	}

	return &PaymentConsumer{
		conn: conn,
		channel: channel,
		processor: processor,
		done: make(chan bool),
		retrier: resilience.NewRetryHandler(3, 2*time.Second),
	}, nil
}

