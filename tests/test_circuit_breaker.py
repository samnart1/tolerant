import time
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'frontend'))
from main import CircuitBreaker, CircuitBreakerOpen

def make_cb(fail_max=3, reset_timeout=30) -> CircuitBreaker:
    return CircuitBreaker("test", fail=fail_max, reset_timeout=reset_timeout)

def trip(cb: CircuitBreaker, n: int = 0):
    n = n or cb.fail_max
    for _ in range(n):
        cb.record_failure()



class TestInitialState:
    def test_starts_closed(self):
        cb = make_cb()
        assert cb.state == CircuitBreaker.CLOSED

    def test_can_execute_when_close(self):
        cb = make_cb()
        assert cb.can_execute() is True

    def test_all_counters_zero(self):
        cb = make_cb()
        assert cb.failure_count == 0
        assert cb.total_calls == 0
        assert cb.total_success == 0
        assert cb.total_failures == 0
        assert cb.total_rejected == 0

    def test_no_state_changes_yet(self):
        cb = make_cb()
        assert cb.state_changes == []


class TestClosedState:
    def test_success_resets_failure_count(self):
        pass

    def test_failures_below_threshold_stay_closed(self):
        pass

    def test_reaching_fail_max_opens_breaker(self):
        pass

    def test_success_increments_counter(self):
        pass

    def test_failure_increments_counter(self):
        pass

    def test_can_execute_increments_total_calls(self):
        pass
