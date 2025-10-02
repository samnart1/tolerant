package pkg

type InventoryResponse struct {
	ProductID			string	`json:"productId"`
	AvailableQuantity	int		`json:"availableQuantity"`
	Available			bool	`json:"available"`
}

type InventoryStatus struct {
	ProductID			string	`json:"productId"`
	TotalQuantity		int		`json:"totalQuantity"`
	AvailableQuantity	int		`json:"availableQuantity"`
	ReservedQuantity	int		`json:"reservedQuantity"`
}

type ReservedRequest struct {
	ProductID	string	`json:"productId" binding:"required"`
	Quantity	int		`json:"quantity" binding:"required,gt=0"`
}

type ReleaseRequest struct {
	ProductID	string	`json:"productId" binding:"required"`
	Quantity	int		`json:"quantity" binding:"required,gt=0"`
}