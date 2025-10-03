package handler

import (
	"net/http"
	"strconv"

	"github.com/samnart1/tolerant/internal/service"

	"github.com/gin-gonic/gin"
)

// type InventoryHandler struct {
// 	service *service.InventoryServcice
// }

type InventoryHandler struct {
	service *service.InventoryService
}

func NewInventoryHandler(service *service.InventoryService) *InventoryHandler {
	return &InventoryHandler{
		service: service,
	}
}

func (h *InventoryHandler) CheckInventory(c *gin.Context) {
	productID := c.Query("productId")
	quantityStr := c.Query("quantity")

	if productID == "" || quantityStr == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "productId and quantity are requried",
		})
		return
	}

	quantity, err := strconv.Atoi(quantityStr)
	if err != nil || quantity <= 0 {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "invalid quantity",
		})
		return
	}

	response, err := h.service.CheckAvailability(c.Request.Context(), productID, quantity) 
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": err.Error(),
		})
		return
	}
	
	c.JSON(http.StatusOK, response)
}

func (h *InventoryHandler) ReserveInventory(c *gin.Context) {
	var request struct {
		ProductID 	string 	`json:"productId" binding:"required"`
		Quantity 	int		`json:"quantity" binding:"required,gt=0"`
	}

	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": err.Error(),
		})
		return
	}

	err := h.service.ReserveInventory(c.Request.Context(), request.ProductID, request.Quantity)
	if err != nil {
		c.JSON(http.StatusConflict, gin.H{
			"error": err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"message": "inventory reserved successfully",
	})
}

func (h *InventoryHandler) ReleaseInventory(c *gin.Context) {
	var request struct {
		ProductID	string	`json:"productId" binding:"required"`
		Quantity	int		`json:"quantity" binding:"required,gt=0"`
	}

	if err := c.ShouldBindJSON(&request); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": err.Error(),
		})
		return
	}

	err := h.service.ReleaseInventory(c.Request.Context(), request.ProductID, request.Quantity)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"message": "inventory released successfully",
	})
}

func (h *InventoryHandler) GetInventoryStatus(c *gin.Context) {
	productID := c.Param("productId")

	if productID == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "productId is required",
		})
		return
	}

	status, err := h.service.GetInventoryStatus(productID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{
			"error": err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, status)
}