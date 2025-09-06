import React from 'react'
import PaymentGatewayInterface from '../components/payments/PaymentGatewayInterface'

export function PaymentGatewayPage() {
  const handlePaymentSuccess = (transaction: any) => {
    console.log('Payment successful:', transaction)
    // Handle successful payment (e.g., update user subscription, show success message)
  }

  const handlePaymentFailure = (error: string, transaction: any) => {
    console.error('Payment failed:', error, transaction)
    // Handle payment failure (e.g., show error message, suggest retry)
  }

  const handleSubscriptionUpdate = (subscription: any) => {
    console.log('Subscription updated:', subscription)
    // Handle subscription changes (e.g., update user profile, refresh UI)
  }

  return (
    <div className="min-h-screen">
      <PaymentGatewayInterface
        onPaymentSuccess={handlePaymentSuccess}
        onPaymentFailure={handlePaymentFailure}
        onSubscriptionUpdate={handleSubscriptionUpdate}
      />
    </div>
  )
}

export default PaymentGatewayPage