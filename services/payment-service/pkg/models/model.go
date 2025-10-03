package models

type PaymentRequest struct {
	OrderID			string	`json:"orderId"`
	ProductID		string	`json:"productId"`
	Quantity		int		`json:"quantity"`
	PaymentMethod	string	`json:"paymentMethod"`
	Amount			float64	`json:"amount"`
}

type PaymentResponse struct {
	OrderID			string	`json:"orderId"`
	Status			string	`json:"status"`
	TransactionID	string	`json:"transactionId,omitempty"`
	Message			string	`json:"message,omitempty"`
}