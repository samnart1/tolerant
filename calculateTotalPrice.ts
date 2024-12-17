function calculateTotalPrice(
  product: { name: string; unitPrice: number },
  quantity: number,
  discount: number
) {
  const priceWithoutDiscount = product.unitPrice * quantity
  const discountAmount = priceWithoutDiscount * discount
  return priceWithoutDiscount - discountAmount
}

function getTotal(
  unitPrice: number,
  quantity: number,
  discount: number
): number {
  const priceWithoutDiscount = unitPrice * quantity
  const discountAmount = priceWithoutDiscount * discount
  return priceWithoutDiscount - discountAmount
}
