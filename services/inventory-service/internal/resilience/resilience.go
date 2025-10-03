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

	//reject if too many requests in halfopen state
	if cb.state == StateHalfOpen && cb.successes >= cb.maxRequests {
		cb.mu.Unlock()
		return errors.New("circuit breaker: too many requests in half-open state")
	}

	cb.mu.Unlock()

	//execute function
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
	if cb.state == StateHalfOpen {
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

func (cb *CircuitBreaker) GetState() State {
	cb.mu.RLock()
	defer cb.mu.RLocker()
	return cb.state
}

//ratelimiter implements token bucket rate limiting
type RateLimiter struct {
	tokens 		int
	maxTokens 	int
	refillRate 	time.Duration
	mu 			sync.Mutex
	lastRefill 	time.Time
}

func NewRateLimiter(maxTokens int, refillRate time.Duration) *RateLimiter {
	return &RateLimiter{
		tokens: maxTokens,
		maxTokens: maxTokens,
		refillRate: refillRate,
		lastRefill: time.Now(),
	}
}

func (rl *RateLimiter) Allow() bool {
	rl.mu.Lock()
	defer rl.mu.Unlock()

	//refill tokens based on time elasped
	now := time.Now()
	elapsed := now.Sub(rl.lastRefill)

	if elapsed >= rl.refillRate {
		tokensToAdd := int(elapsed / rl.refillRate)
		rl.tokens = min(rl.tokens+tokensToAdd, rl.maxTokens)
		rl.lastRefill = now
	}

	if rl.tokens > 0 {
		rl.tokens--
		return  true
	}

	return false
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}

//retry implements exponential backoff retry logic
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