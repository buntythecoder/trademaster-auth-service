import React, { useRef, useEffect } from 'react'

interface ChartDataPoint {
  label: string
  value: number
  color: string
}

interface SimpleChartProps {
  data: ChartDataPoint[]
  type: 'pie' | 'bar' | 'line'
  width?: number
  height?: number
  title?: string
}

export function SimpleChart({ data, type, width = 300, height = 300, title }: SimpleChartProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas || !data.length) return

    const ctx = canvas.getContext('2d')
    if (!ctx) return

    // Set canvas size
    canvas.width = width
    canvas.height = height
    canvas.style.width = width + 'px'
    canvas.style.height = height + 'px'

    // Clear canvas
    ctx.fillStyle = 'rgba(15, 23, 42, 0.9)'
    ctx.fillRect(0, 0, width, height)

    if (type === 'pie') {
      drawPieChart(ctx, data, width, height)
    } else if (type === 'bar') {
      drawBarChart(ctx, data, width, height)
    } else if (type === 'line') {
      drawLineChart(ctx, data, width, height)
    }
  }, [data, type, width, height])

  const drawPieChart = (ctx: CanvasRenderingContext2D, data: ChartDataPoint[], w: number, h: number) => {
    const centerX = w / 2
    const centerY = h / 2
    const radius = Math.min(w, h) / 2 - 40

    const total = data.reduce((sum, item) => sum + item.value, 0)
    let currentAngle = -Math.PI / 2 // Start from top

    data.forEach((item) => {
      const sliceAngle = (item.value / total) * 2 * Math.PI
      
      // Draw slice
      ctx.beginPath()
      ctx.moveTo(centerX, centerY)
      ctx.arc(centerX, centerY, radius, currentAngle, currentAngle + sliceAngle)
      ctx.closePath()
      ctx.fillStyle = item.color
      ctx.fill()

      // Draw label
      const labelAngle = currentAngle + sliceAngle / 2
      const labelX = centerX + Math.cos(labelAngle) * (radius * 0.7)
      const labelY = centerY + Math.sin(labelAngle) * (radius * 0.7)
      
      ctx.fillStyle = '#fff'
      ctx.font = '12px Inter, system-ui'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      
      const percentage = ((item.value / total) * 100).toFixed(1) + '%'
      ctx.fillText(percentage, labelX, labelY)

      currentAngle += sliceAngle
    })
  }

  const drawBarChart = (ctx: CanvasRenderingContext2D, data: ChartDataPoint[], w: number, h: number) => {
    const padding = 40
    const chartWidth = w - padding * 2
    const chartHeight = h - padding * 2
    const barWidth = chartWidth / data.length * 0.8
    const barSpacing = chartWidth / data.length

    const maxValue = Math.max(...data.map(d => d.value))

    data.forEach((item, index) => {
      const barHeight = (item.value / maxValue) * chartHeight
      const x = padding + index * barSpacing + barSpacing * 0.1
      const y = padding + chartHeight - barHeight

      // Draw bar
      ctx.fillStyle = item.color
      ctx.fillRect(x, y, barWidth, barHeight)

      // Draw label
      ctx.fillStyle = '#94a3b8'
      ctx.font = '10px Inter, system-ui'
      ctx.textAlign = 'center'
      ctx.fillText(item.label, x + barWidth / 2, h - 10)

      // Draw value
      ctx.fillStyle = '#fff'
      ctx.font = '11px Inter, system-ui'
      ctx.fillText(item.value.toFixed(1), x + barWidth / 2, y - 5)
    })
  }

  const drawLineChart = (ctx: CanvasRenderingContext2D, data: ChartDataPoint[], w: number, h: number) => {
    const padding = 40
    const chartWidth = w - padding * 2
    const chartHeight = h - padding * 2

    const maxValue = Math.max(...data.map(d => d.value))
    const minValue = Math.min(...data.map(d => d.value))
    const valueRange = maxValue - minValue || 1

    // Draw grid
    ctx.strokeStyle = 'rgba(71, 85, 105, 0.3)'
    ctx.lineWidth = 1
    
    for (let i = 0; i <= 5; i++) {
      const y = padding + (chartHeight / 5) * i
      ctx.beginPath()
      ctx.moveTo(padding, y)
      ctx.lineTo(padding + chartWidth, y)
      ctx.stroke()
    }

    // Draw line
    ctx.strokeStyle = '#8b5cf6'
    ctx.lineWidth = 2
    ctx.beginPath()

    data.forEach((item, index) => {
      const x = padding + (index / (data.length - 1)) * chartWidth
      const y = padding + chartHeight - ((item.value - minValue) / valueRange) * chartHeight

      if (index === 0) {
        ctx.moveTo(x, y)
      } else {
        ctx.lineTo(x, y)
      }

      // Draw data points
      ctx.fillStyle = item.color
      ctx.beginPath()
      ctx.arc(x, y, 4, 0, 2 * Math.PI)
      ctx.fill()
    })

    ctx.stroke()

    // Draw labels
    ctx.fillStyle = '#94a3b8'
    ctx.font = '10px Inter, system-ui'
    ctx.textAlign = 'center'
    
    data.forEach((item, index) => {
      const x = padding + (index / (data.length - 1)) * chartWidth
      ctx.fillText(item.label, x, h - 10)
    })
  }

  return (
    <div className="flex flex-col items-center">
      {title && <h4 className="text-sm font-semibold text-slate-300 mb-2">{title}</h4>}
      <canvas
        ref={canvasRef}
        className="rounded-lg bg-slate-900/50"
        style={{ maxWidth: '100%', height: 'auto' }}
      />
    </div>
  )
}