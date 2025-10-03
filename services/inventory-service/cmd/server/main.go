package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/samnart1/tolerant/internal/handler"
	"github.com/samnart1/tolerant/internal/service"
	"github.com/samnart1/tolerant/pkg/metrics"
)

func main() {
	//initialize metrics
	metrics.Init()
	//create inventory service with resilience patterns
	inventoryService := service.NewInventoryService()
	//create http handler
	inventoryHandler := handler.NewInventoryHandler(inventoryService)
	//setup router
	router := gin.Default()
	//health check
	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "UP"})
	})

	//metrics endpoint
	router.GET("/metrics", gin.WrapH(promhttp.Handler()))

	//api routes
	api := router.Group("/api")
	{
		inventory := api.Group("/inventory")
		{
			inventory.GET("/check", inventoryHandler.CheckInventory)
			inventory.POST("/reserve", inventoryHandler.ReserveInventory)
			inventory.POST("/reserve", inventoryHandler.ReleaseInventory)
			inventory.GET("/status/:productId", inventoryHandler.GetInventoryStatus)
		}
	}

	//create server
	port := getEnv("PORT", "8081")
	srv := &http.Server{
		Addr: fmt.Sprintf(":%s", port),
		Handler: router,
		ReadTimeout: 10 * time.Second,
		WriteTimeout: 10 * time.Second,
		IdleTimeout: 60 * time.Second,
	}

	//start service in goroutine
	go func() {
		log.Printf("Starting inventory service on port %s", port)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Failed to start server: %v", err)
		}
	}()

	//lets gracefully shut it down
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	log.Println("Gracefully shutting down server...............")
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		
	}
}