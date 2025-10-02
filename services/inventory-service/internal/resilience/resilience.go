package resilience

import (
	"errors"
	"sync"
	"time"
)

type State int //circuit breaker states

const (
	StateClosed State = iota
	StateOpen
	StateHalfOpen
)

type CircuitBreaker struct {
	maxFailures		int
	maxRequests		int
	timeout			time.Duration
	state			State
	failures		int
	successes		int
	lastFailTime	time.Time
	mu				sync.RWMutex
}

func NewCircuitBreaker(maxFailures, maxRequests int, timeout time.Duration) *CircuitBreaker {
	return &CircuitBreaker{
		maxFailures: maxFailures,
		maxRequests: maxRequests,
		timeout: timeout,
		state: StateClosed,
	}
}

func (cb *CircuitBreaker) Execute(fn func() error) error {
	cb.mu.Lock()

	//should we move from open to half-open?
	if cb.state == StateOpen {
		if time.Since(cb.lastFailTime) > cb.timeout {
			cb.state = StateHalfOpen
			cb.successes = 0
		} else {
			cb.mu.Unlock()
			return errors.New("circuit breaker is open")
		}
	}

	
}