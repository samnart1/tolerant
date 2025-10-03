package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/samnart/thesis/internal/consumer"
	"github.com/samnart/thesis/internal/processor"
	"github.com/samnart/thesis/pkg/metrics"
)

func main() {
	metrics.Init()
	paymentProcessor := processor.NewPaymentProcessor()
	rabbitMQURL := getEnv("RABBITMQ_URL", "amqp://admin:admin@localhost:5672/")
	paymentConsumer, err := consumer.NewPaymentConsumer(rabbitMQURL, paymentProcessor)
	if err != nil {
		log.Fatalf("failed to create payment consumer,: %v", err)
	}

	//stat consuming messages
	go func() {
		log.Println("starting payment message consumer..........")
		if err := paymentConsumer.Start(); err != nil {
			log.Fatalf("Failed to start consumer: %v", err)
		}
	}()

	//setup http server for health and metrics
	router := gin.Default()

	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{
			"status": "UP",
		})
	})

	router.GET("/metrics", gin.WrapH(promhttp.Handler()))

	//start http server
	port := getEnv("PORT", "8082")
	srv := &http.Server{
		Addr: ":" + port,
		Handler: router,
	}

	go func() {
		log.Printf("STarting payment service http server on port %s", port)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("failed to start http server: %v", err)
		}
	}()

	//shutting  down gracefully
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	log.Println("shutting down payment service gracefully")

	paymentConsumer.Stop()

	//shutdown http server
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		log.Fatalf("server forced to shutdown: %v", err)
	}

	log.Println("payment service exited")
}

func getEnv(key, defaultKey string) string {
	if value := os.Getenv(key); value != "" {
		return value
	} 
	return defaultKey
}
