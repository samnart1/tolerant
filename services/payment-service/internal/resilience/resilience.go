package resilience

import (
	"errors"
	"sync"
	"time"
)

//circuit breaker
type State int

const (
	StateClosed State = iota
	StateOpen
	StateHalfOpen
)

type CircuitBreaker struct {
	maxFailures 	int
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

	if cb.state == StateOpen {
		if time.Since(cb.lastFailTime) > cb.timeout {
			cb.state = StateHalfOpen
			cb.successes = 0

		} else {
			cb.mu.Unlock()
			return errors.New("circuit breaker is open")
		}
	}

	if cb.state == StateHalfOpen && cb.successes >= cb.maxRequests {
		cb.mu.Unlock()
		return  errors.New("circuit breaker: too many requests in half-open state")
	}

	cb.mu.Unlock()

	err := fn()

	cb.mu.Lock()
	defer cb.mu.Unlock()

	if err != nil {
		cb.onFailure()
		return err
	}

	cb.onSuccess()
	return nil
}

func (cb *CircuitBreaker) onSuccess() {
	if cb.state == StateOpen {
		cb.successes++
		if cb.successes >= cb.maxRequests {
			cb.state = StateClosed
			cb.failures = 0
		}
	} else {
		cb.failures = 0
	}
}

func (cb *CircuitBreaker) onFailure() {
	cb.failures++
	cb.lastFailTime = time.Now()

	if cb.failures >= cb.maxFailures {
		cb.state = StateOpen
	}
}

// retryhandler for message processing
type RetryHandler struct {
	maxRetries	int
	backoff		time.Duration
}

func NewRetryHandler(maxRetries int, backoff time.Duration) *RetryHandler {
	return &RetryHandler{
		maxRetries: maxRetries,
		backoff: backoff,
	}
}

func (rh *RetryHandler) Execute(fn func() error) error {
	var lastErr error

	for attempt := 0; attempt <= rh.maxRetries; attempt++ {
		err := fn()
		if err == nil {
			return nil
		}

		lastErr = err

		if attempt < rh.maxRetries {
			wait := time.Duration(attempt+1) * rh.backoff
			time.Sleep(wait)
		}
	}

	return lastErr
}

type RetryConfig struct {
	MaxAttempts			int
	InitialDelay		time.Duration
	MaxDelay			time.Duration
	BackoffMultiplier	float64
}

func Retry(config RetryConfig, fn func() error) error {
	var lastErr error
	delay := config.InitialDelay

	for attempt := 0; attempt < config.MaxAttempts; attempt++ {
		err := fn()
		if err == nil {
			return nil
		}

		lastErr = err

		if attempt < config.MaxAttempts-1 {
			time.Sleep(delay)
			delay = time.Duration(float64(delay) * config.BackoffMultiplier)
			if delay > config.MaxDelay {
				delay = config.MaxDelay
			}
		}
	}

	return lastErr

}