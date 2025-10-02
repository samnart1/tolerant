package pkg

import (
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
)

var (
	inventoryCheckDuration = promauto.NewHistogram(prometheus.HistogramOpts{
		Name:		"inventory_check_duration_seconds",
		Help:		"Duration of inventory check operations",
		Buckets:	prometheus.DefBuckets,
	})

	inventoryReserveDuration = promauto.NewHistogram(prometheus.HistogramOpts{
		Name:		"inventory_reserve_duration_seconds",
		Help:		"Duration of inventory reserve operations",
		Buckets:	prometheus.DefBuckets,
	})

	inventoryCheckSuccess = promauto.NewCounter(prometheus.CounterOpts{
		Name: "inventory_check_success_total",
		Help: "Total number of successfull inventory cheks",
	})

	inventoryCheckFailure = promauto.NewCounter(prometheus.CounterOpts{
		Name: "inventory_check_failure_total",
		Help: "Total number of failed inventory checks",
	})

	inventoryReserveSuccess = promauto.NewCounter(prometheus.CounterOpts{
		Name: "inventory_reserve_success_total",
		Help: "Total number of successful inventory reservations",
	})

	inventoryReserveFailure = promauto.NewCounter(prometheus.CounterOpts{
		Name: "inventory_reserve_failure_total",
		Help: "Total number of failed inventory reservations",
	})

	inventoryRateLimitHits = promauto.NewCounter(prometheus.CounterOpts{
		Name: "inventory_rate_limit_hits_total",
		Help: "Total number of rate limit hits",
	})
)

func Init() {}

func RecordInventoryCheckDuration(duration float64) {
	inventoryCheckDuration.Observe(duration)
}

func RecordInventoryReserveDuration(duration float64) {
	inventoryReserveDuration.Observe(duration)
}

func IncInventoryCheckSuccessCounter() {
	inventoryCheckSuccess.Inc()
}

func IncInventoryCheckFailureCounter() {
	inventoryCheckFailure.Inc()
}

func IncInventoryReserveSuccessCounter() {
	inventoryReserveSuccess.Inc()
}

func IncInventoryReserveFailureCounter() {
	inventoryReserveFailure.Inc()
}

func IncInventoryRateLimitCounter() {
	inventoryRateLimitHits.Inc()
}