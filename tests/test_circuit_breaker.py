import time
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'frontend'))
from main import CircuitBreaker, CircuitBreakerOpen

def make_cb(fail_max=3, reset_timeout=30) -> CircuitBreaker:
    return CircuitBreaker("test", fail_max=fail_max, reset_timeout=reset_timeout)

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
        assert cb.total_successes == 0
        assert cb.total_failures == 0
        assert cb.total_rejected == 0

    def test_no_state_changes_yet(self):
        cb = make_cb()
        assert cb.state_changes == []


class TestClosedState:
    def test_success_resets_failure_count(self):
        cb = make_cb(fail_max=3)
        cb.record_failure()
        cb.record_failure()
        cb.record_success()
        assert cb.failure_count == 0
        assert cb.state == CircuitBreaker.CLOSED

    def test_failures_below_threshold_stay_closed(self):
        cb = make_cb(fail_max=3)
        cb.record_failure()
        cb.record_failure()
        assert cb.state == CircuitBreaker.CLOSED

    def test_reaching_fail_max_opens_breaker(self):
        cb = make_cb(fail_max=3)
        trip(cb)
        assert cb.state == CircuitBreaker.OPEN

    def test_success_increments_counter(self):
        cb = make_cb(fail_max=3)
        cb.record_success()
        assert cb.total_successes == 1

    def test_failure_increments_counter(self):
        cb = make_cb(fail_max=3)
        cb.record_failure()
        assert cb.total_failures == 1

    def test_can_execute_increments_total_calls(self):
        cb = make_cb(fail_max=3)
        cb.can_execute()
        cb.can_execute()
        assert cb.total_calls == 2


class TestClosedToOpen:
    def test_state_changes_logged(self):
        cb = make_cb(fail_max=2)
        trip(cb)
        assert len(cb.state_changes) == 1
        change = cb.state_changes[0]
        assert change["from"] == CircuitBreaker.CLOSED
        assert change["to"] == CircuitBreaker.OPEN

    def test_state_change_records_failure_count(self):
        cb = make_cb(fail_max=2)
        trip(cb)
        assert cb.state_changes[0]["failure_count"] == 2

    def test_last_failure_time_set(self):
        cb = make_cb(fail_max=1)
        before = time.time()
        cb.record_failure()
        after = time.time()
        assert before <= cb.last_failure_time <= after

class TestOpenToClose:
    def test_rejects_calls_when_open(self):
        cb = make_cb(fail_max=1)
        trip(cb)
        assert cb.can_execute() is False

    def test_rejected_calls_counted(self):
        cb = make_cb(fail_max=1)
        trip(cb)
        cb.can_execute()
        cb.can_execute()
        assert cb.total_rejected == 2

    def test_total_calls_still_increments_when_rejected(self):
        cb = make_cb(fail_max=1)
        trip(cb)
        cb.can_execute()
        assert cb.total_calls == 1

    def test_stays_open_before_timeout(self):
        cb = make_cb(fail_max=1, reset_timeout=60)
        trip(cb)
        assert cb.state == CircuitBreaker.OPEN
        assert cb.can_execute() is False
        assert cb.state == CircuitBreaker.OPEN

class TestOpenToHalfOpen:
    def test_transitions_to_half_open_after_timeout(self, monkeypatch):
        cb = make_cb(fail_max=1, reset_timeout=1)
        trip(cb)
        
        monkeypatch.setattr(time, "time", lambda: cb.last_failure_time+2)

        assert cb.can_execute() is True
        assert cb.state == CircuitBreaker.HALF_OPEN

    def test_state_change_logged_on_half_open(self, monkeypatch):
        cb = make_cb(fail_max=1, reset_timeout=1)
        trip(cb)

        monkeypatch.setattr(time, "time", lambda: cb.last_failure_time+2)
        cb.can_execute()

        states = [(s["from"], s["to"]) for s in cb.state_changes]
        assert (CircuitBreaker.OPEN, CircuitBreaker.HALF_OPEN) in states


class TestHalfOpenState:
    def _get_half_open_cb(self, monkeypatch, fail_max=1, reset_timeout=1):
        cb = make_cb(fail_max=fail_max, reset_timeout=reset_timeout)
        trip(cb)
        monkeypatch.setattr(time, "time", lambda: cb.last_failure_time+2)
        cb.can_execute()
        return cb

    def test_failure_in_half_open_reopens(self, monkeypatch):
        cb = self._get_half_open_cb(monkeypatch)
        cb.record_failure()
        assert cb.state == CircuitBreaker.OPEN

    def test_single_success_in_half_open_stays_half_open(self, monkeypatch):
        cb = make_cb(fail_max=1, reset_timeout=1)
        trip(cb)
        monkeypatch.setattr(time, "time", lambda: cb.last_failure_time+2)
        cb.can_execute()
        cb.record_success()
        assert cb.state == CircuitBreaker.HALF_OPEN


    def test_two_success_close_breaker(self, monkeypatch):
        cb = make_cb(fail_max=1, reset_timeout=1)
        trip(cb)
        monkeypatch.setattr(time, "time", lambda: cb.last_failure_time + 2)
        cb.can_execute()
        cb.record_success()
        cb.record_success()
        assert cb.state == CircuitBreaker.CLOSED


    def test_failure_count_reset_on_close(self, monkeypatch):
        cb = make_cb(fail_max=1, reset_timeout=1)
        trip(cb)
        monkeypatch.setattr(time, "time", lambda: cb.last_failure_time+2)
        cb.can_execute()
        cb.record_success()
        cb.record_success()
        assert cb.failure_count == 0


    def test_full_state_change_sequence_logged(self, monkeypatch):
        cb = make_cb(fail_max=1, reset_timeout=1)
        trip(cb)
        monkeypatch.setattr(time, "time", lambda: cb.last_failure_time+2)
        cb.can_execute()
        cb.record_success()
        cb.record_success()
        
        transitions = [(s["from"], s["to"]) for s in cb.state_changes]
        assert transitions == [
            (CircuitBreaker.CLOSED, CircuitBreaker.OPEN),
            (CircuitBreaker.OPEN, CircuitBreaker.HALF_OPEN),
            (CircuitBreaker.HALF_OPEN, CircuitBreaker.CLOSED)
        ]




class TestMetrics:
    def test_get_metric_keys(self):
        cb = make_cb()
        m = cb.get_metrics()
        assert set(m.keys()) == {
            "name", "state", "failure_count", 
            "total_calls", "total_successes", "total_failures",
            "total_rejected", "state_changes"
        }

    def test_metrics_reflect_activity(self):
        cb = make_cb(fail_max=2, reset_timeout=1)
        cb.can_execute(); cb.record_success()
        cb.can_execute(); cb.record_failure()
        cb.can_execute(); cb.record_failure()
        cb.can_execute();

        m = cb.get_metrics()
        assert m["total_calls"] == 4
        assert m["total_successes"] == 1
        assert m["total_failures"] == 2
        assert m["total_rejected"] == 1
        assert m["state"] == CircuitBreaker.OPEN

    def test_name_preserved(self):
        cb = CircuitBreaker("my-service", fail_max=5, reset_timeout=10)
        assert cb.get_metrics()["name"] == "my-service"



class TestEdgeCases:
    def test_fail_max_one(self):
        cb = make_cb(fail_max=1)
        cb.record_failure()
        assert cb.state == CircuitBreaker.OPEN


    def test_multiple_trips_and_recoveries(self, monkeypatch):
        cb = make_cb(fail_max=1, reset_timeout=1)

        for _ in range(3):
            cb.record_failure()
            assert cb.state == CircuitBreaker.OPEN

            monkeypatch.setattr(time, "time", lambda: cb.last_failure_time +2)
            cb.can_execute()
            cb.record_success()
            cb.record_success()
            assert cb.state == CircuitBreaker.CLOSED


    def test_success_on_closed_does_not_create_state_change(self):
        cb = make_cb()
        cb.record_success()
        assert cb.state_changes == []


    def test_half_open_reopens_and_can_recover_again(self, monkeypatch):
        cb = make_cb(fail_max=1, reset_timeout=1)
        trip(cb)

        monkeypatch.setattr(time, "time", lambda: cb.last_failure_time + 2)
        cb.can_execute()
        cb.record_failure()

        monkeypatch.setattr(time, "time", lambda: cb.last_failure_time + 2)
        cb.can_execute()
        cb.record_success()
        cb.record_success()
        assert cb.state == CircuitBreaker.CLOSED
