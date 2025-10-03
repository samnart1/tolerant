package service

import (
	"context"
	"errors"
	"fmt"
	"log"
	"sync"
	"time"

	"github.com/samnart1/tolerant/internal/resilience"
	"github.com/samnart1/tolerant/pkg/metrics"
	"github.com/samnart1/tolerant/pkg/models"
)

type InventoryService struct {
	inventory		map[string]*InventoryItem
	mu				sync.RWMutex
	circuitBreaker	*resilience.CircuitBreaker
	rateLimiter		*resilience.RateLimiter
}

type InventoryItem struct {
	ProductID			string
	TotalQuantity		int
	AvailableQuantity	int
	ReservedQualityt	int
	mu					sync.RWMutex
}

func NewInventoryService() *InventoryService {
	service := &InventoryService{
		inventory: make(map[string]*InventoryItem),
		circuitBreaker: resilience.NewCircuitBreaker(5, 10, 30*time.Second),
		rateLimiter: resilience.NewRateLimiter(100, time.Second),
	}

	//initialize sample products
	service.initializeSampleInventory()

	return service
}

func (s *InventoryService) initializeSampleInventory() {
	sampleProducts := []struct{
		id			string
		quantity	int
	}{
		{"PROD-001", 100},
		{"PROD-002", 050},
		{"PROD-003", 200},
		{"PROD-004", 075},
		{"PROD-005", 150},
	}

	for _, p := range sampleProducts {
		s.inventory[p.id] = &InventoryItem{
			ProductID: p.id,
			TotalQuantity: p.quantity,
			AvailableQuantity: p.quantity,
			ReservedQualityt: 0,
		}
	}

	log.Printf("Initialized inventory with %d products", len(sampleProducts))
}

func (s *InventoryService) CheckAvailability(ctx context.Context, productID string, quantity int) (*models.InventoryResponse, error) {
	startTime := time.Now()
	defer func() {
		metrics.RecordInventoryCheckDuration(time.Since(startTime).Seconds())
	}()

	//rate limiting
	if !s.rateLimiter.Allow() {
		metrics.IncInventoryRateLimitCounter()
		return nil, errors.New("rate limit exceeded")
	}

	//circuit breaker
	err := s.circuitBreaker.Execute(func() error {
		return s.checkAvailabilityInternal(productID, quantity)
	})

	if err != nil {
		metrics.IncInventoryCheckFailureCounter()
		return &models.InventoryResponse{
			ProductID: 			productID,
			AvailableQuantity: 	0,
			Available: 			false,
		}, err
	}

	s.mu.RLock()
	item, exists := s.inventory[productID]
	s.mu.RUnlock()

	if !exists {
		metrics.IncInventoryCheckFailureCounter()
		return &models.InventoryResponse{
			ProductID: 			productID,
			AvailableQuantity: 	0,
			Available: 			false,
		}, fmt.Errorf("product %s not found", productID)
	}

	item.mu.RLock()
	available := item.AvailableQuantity >= quantity
	availQty := item.AvailableQuantity
	item.mu.RUnlock()

	metrics.IncInventoryCheckSuccessCounter()

	return &models.InventoryResponse{
		ProductID: productID,
		AvailableQuantity: availQty,
		Available: available,
	}, nil
}

func (s *InventoryService) checkAvailabilityInternal(productID string, quantity int) error {
	s.mu.RLock()
	item, exists := s.inventory[productID]
	s.mu.RUnlock()

	if !exists {
		return fmt.Errorf("product not found: %s", productID)
	}

	item.mu.RLock()
	defer item.mu.RUnlock()

	if item.AvailableQuantity < quantity {
		return fmt.Errorf("insufficient inventory for product %s", productID)


	}

	return nil
}

func (s *InventoryService) ReserveInventory(ctx context.Context, productID string, quantity int) error {
	startTime := time.Now()
	defer func() {
		metrics.RecordInventoryReserveDuration(time.Since(startTime).Seconds())
	}()

	s.mu.RLock()
	item, exists := s.inventory[productID]
	s.mu.RUnlock()

	if !exists {
		metrics.IncInventoryReserveFailureCounter()
		return fmt.Errorf("product not found: %s", productID)
	}

	item.mu.Unlock()

	if item.AvailableQuantity < quantity {
		metrics.IncInventoryReserveFailureCounter()
		return fmt.Errorf("insufficient inventory: need %d, have %d", quantity, item.AvailableQuantity)
	}

	item.AvailableQuantity -= quantity
	item.ReservedQualityt += quantity

	metrics.IncInventoryReserveSuccessCounter()
	log.Printf("Reserved %d units of %s. Available: %d, Reserved: %d", quantity, productID, item.AvailableQuantity, item.ReservedQualityt)

	return  nil
}

func (s *InventoryService) ReleaseInventory(ctx context.Context, productID string, quantity int) error {
	s.mu.RLock()
	item, exists := s.inventory[productID]
	s.mu.RUnlock()

	if !exists {
		return fmt.Errorf("product not found: %s", productID)
	}

	item.mu.Lock()
	defer item.mu.Unlock()

	if item.ReservedQualityt < quantity {
		return fmt.Errorf("cannot release %d units: only %d reserved", quantity, item.ReservedQualityt)
	}

	item.ReservedQualityt -= quantity
	item.AvailableQuantity += quantity

	log.Printf("Released %d units of %s. Available: %d, Reserved: %d", quantity, productID, item.AvailableQuantity, item.ReservedQualityt)

	return nil
}

func (s *InventoryService) GetInventoryStatus(productID string) (*models.InventoryStatus, error) {
	s.mu.RLock()
	item, exists := s.inventory[productID]
	s.mu.RUnlock()

	if !exists {
		return nil, fmt.Errorf("product not found: %s", productID)
	}

	item.mu.RLock()
	defer item.mu.RUnlock()

	return &models.InventoryStatus{
		ProductID: item.ProductID,
		TotalQuantity: item.TotalQuantity,
		AvailableQuantity: item.AvailableQuantity,
		ReservedQuantity: item.ReservedQualityt,
	}, nil
}