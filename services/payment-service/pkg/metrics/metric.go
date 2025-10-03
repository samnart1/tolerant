package metrics

import (
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
)

var (
	paymentProcessDuration = promauto.NewHistogram(prometheus.HistogramOpts{
		Name:		"payment_process_duration_seconds",
		Help:		"Duration of payment processing",
		Buckets:	prometheus.DefBuckets,
	})

	paymentSuccess = promauto.NewCounter(prometheus.CounterOpts{
		Name:	"payment_success_total",
		Help:	"Total number of successfuly payments",
	})

	paymentFailure = promauto.NewCounter(prometheus.CounterOpts{
		Name:	"payment_failure_total",
		Help:	"Total number of failed payments",
	})

	paymentRetries = promauto.NewCounter(prometheus.CounterOpts{
		Name:	"payment_retries_total",
		Help:	"Total number of payment retries",
	})
)

func Init(){}

func RecordPaymentProcessDuration(duration float64) {
	paymentProcessDuration.Observe(duration);
}

func IncPaymentSuccessCounter() {
	paymentSuccess.Inc();
}

func IncPaymentFailureCounter() {
	paymentFailure.Inc()
}

func IncPaymentRetryCounter() {
	paymentRetries.Inc()
}