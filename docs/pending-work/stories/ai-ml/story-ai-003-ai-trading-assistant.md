# Story AI-003: AI Trading Assistant & Recommendation Engine

## Epic
Epic 3: AI Integration & Behavioral Analytics

## Story Overview
**As a** TradeMaster user  
**I want** an intelligent AI assistant that provides personalized trading recommendations  
**So that** I can make better-informed trading decisions and improve my portfolio performance

## Business Value
- **Premium Revenue**: Core feature for ₹2,999/month AI Premium tier
- **User Engagement**: 60% increase in daily active usage with AI recommendations
- **Performance Improvement**: 25% average improvement in user trading returns
- **Competitive Moat**: Advanced AI capabilities difficult for competitors to replicate

## Technical Requirements

### AI Assistant Architecture
```python
# AI Trading Assistant Core System
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple, Union
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
import openai
from transformers import pipeline, AutoTokenizer, AutoModelForSequenceClassification
import yfinance as yf
from sklearn.ensemble import RandomForestRegressor
import tensorflow as tf
from tensorflow import keras
import asyncio
import aiohttp
from enum import Enum

class RecommendationType(Enum):
    BUY = "buy"
    SELL = "sell"
    HOLD = "hold"
    AVOID = "avoid"
    REDUCE = "reduce_position"
    INCREASE = "increase_position"

class ConfidenceLevel(Enum):
    VERY_LOW = "very_low"
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"
    VERY_HIGH = "very_high"

@dataclass
class TradingRecommendation:
    """Trading recommendation data structure"""
    symbol: str
    recommendation_type: RecommendationType
    confidence_level: ConfidenceLevel
    confidence_score: float
    target_price: Optional[float]
    stop_loss: Optional[float]
    time_horizon: str  # 'short', 'medium', 'long'
    reasoning: str
    risk_level: str
    expected_return: float
    probability_of_success: float
    supporting_factors: List[str]
    risk_factors: List[str]
    market_context: Dict
    personalization_factors: Dict
    timestamp: datetime

@dataclass
class MarketContext:
    """Market context for recommendations"""
    overall_sentiment: float
    sector_sentiment: Dict[str, float]
    volatility_regime: str  # 'low', 'medium', 'high'
    market_trend: str  # 'bullish', 'bearish', 'sideways'
    economic_indicators: Dict[str, float]
    news_sentiment: float
    technical_indicators: Dict[str, float]

class AITradingAssistant:
    """Main AI Trading Assistant with multi-model intelligence"""
    
    def __init__(self):
        self.openai_client = openai.OpenAI()
        self.sentiment_analyzer = pipeline(
            "sentiment-analysis",
            model="finbert/finbert",
            tokenizer="finbert/finbert"
        )
        self.price_predictor = self._build_price_prediction_model()
        self.risk_assessor = RandomForestRegressor(n_estimators=100, random_state=42)
        self.recommendation_engine = RecommendationEngine()
        self.market_analyzer = MarketContextAnalyzer()
        self.personalization_engine = PersonalizationEngine()
        
    def _build_price_prediction_model(self) -> keras.Model:
        """Build LSTM model for price prediction"""
        model = keras.Sequential([
            keras.layers.LSTM(50, return_sequences=True, input_shape=(60, 5)),
            keras.layers.Dropout(0.2),
            keras.layers.LSTM(50, return_sequences=True),
            keras.layers.Dropout(0.2),
            keras.layers.LSTM(50),
            keras.layers.Dropout(0.2),
            keras.layers.Dense(25),
            keras.layers.Dense(1)
        ])
        
        model.compile(optimizer='adam', loss='mean_squared_error')
        return model
    
    async def get_trading_recommendations(
        self,
        user_id: str,
        symbols: List[str],
        portfolio_context: Dict,
        user_preferences: Dict
    ) -> List[TradingRecommendation]:
        """Generate personalized trading recommendations"""
        
        # Gather market context
        market_context = await self.market_analyzer.analyze_market_context(symbols)
        
        # Get user personalization factors
        personalization_factors = await self.personalization_engine.get_user_factors(user_id)
        
        recommendations = []
        
        for symbol in symbols:
            try:
                # Multi-model analysis
                analysis_tasks = [
                    self._analyze_fundamentals(symbol),
                    self._analyze_technicals(symbol),
                    self._analyze_sentiment(symbol),
                    self._predict_price_movement(symbol),
                    self._assess_risk(symbol, portfolio_context)
                ]
                
                fundamental_score, technical_score, sentiment_score, price_prediction, risk_assessment = await asyncio.gather(*analysis_tasks)
                
                # Generate recommendation using ensemble approach
                recommendation = await self.recommendation_engine.generate_recommendation(
                    symbol=symbol,
                    fundamental_score=fundamental_score,
                    technical_score=technical_score,
                    sentiment_score=sentiment_score,
                    price_prediction=price_prediction,
                    risk_assessment=risk_assessment,
                    market_context=market_context,
                    personalization_factors=personalization_factors,
                    portfolio_context=portfolio_context
                )
                
                # Add natural language reasoning
                reasoning = await self._generate_reasoning(symbol, recommendation, market_context)
                recommendation.reasoning = reasoning
                
                recommendations.append(recommendation)
                
            except Exception as e:
                print(f"Error analyzing {symbol}: {e}")
                continue
        
        # Rank and filter recommendations
        recommendations = self._rank_recommendations(recommendations, portfolio_context)
        
        return recommendations[:10]  # Return top 10 recommendations
    
    async def _analyze_fundamentals(self, symbol: str) -> Dict:
        """Analyze fundamental indicators"""
        try:
            stock = yf.Ticker(symbol)
            info = stock.info
            financials = stock.financials
            
            # Key fundamental metrics
            pe_ratio = info.get('forwardPE', info.get('trailingPE'))
            peg_ratio = info.get('pegRatio')
            debt_to_equity = info.get('debtToEquity')
            roe = info.get('returnOnEquity')
            profit_margins = info.get('profitMargins')
            revenue_growth = info.get('revenueGrowth')
            
            # Calculate fundamental score
            score_components = []
            
            # P/E ratio scoring (lower is better, but not too low)
            if pe_ratio and 5 <= pe_ratio <= 25:
                score_components.append(0.8)
            elif pe_ratio and pe_ratio < 5:
                score_components.append(0.3)  # Too low might indicate problems
            elif pe_ratio and pe_ratio > 25:
                score_components.append(0.2)  # Too high, overvalued
            else:
                score_components.append(0.5)  # No data
            
            # PEG ratio scoring
            if peg_ratio and 0.5 <= peg_ratio <= 1.5:
                score_components.append(0.8)
            elif peg_ratio and peg_ratio < 0.5:
                score_components.append(0.6)
            else:
                score_components.append(0.4)
            
            # ROE scoring
            if roe and roe > 0.15:
                score_components.append(0.9)
            elif roe and roe > 0.10:
                score_components.append(0.7)
            else:
                score_components.append(0.4)
            
            # Revenue growth scoring
            if revenue_growth and revenue_growth > 0.15:
                score_components.append(0.9)
            elif revenue_growth and revenue_growth > 0.05:
                score_components.append(0.7)
            else:
                score_components.append(0.4)
            
            fundamental_score = np.mean(score_components)
            
            return {
                'score': fundamental_score,
                'pe_ratio': pe_ratio,
                'peg_ratio': peg_ratio,
                'debt_to_equity': debt_to_equity,
                'roe': roe,
                'profit_margins': profit_margins,
                'revenue_growth': revenue_growth,
                'market_cap': info.get('marketCap'),
                'sector': info.get('sector'),
                'industry': info.get('industry')
            }
            
        except Exception as e:
            print(f"Error in fundamental analysis for {symbol}: {e}")
            return {'score': 0.5}
    
    async def _analyze_technicals(self, symbol: str) -> Dict:
        """Analyze technical indicators"""
        try:
            stock = yf.Ticker(symbol)
            hist = stock.history(period="3mo", interval="1d")
            
            if hist.empty:
                return {'score': 0.5}
            
            # Calculate technical indicators
            close_prices = hist['Close']
            high_prices = hist['High']
            low_prices = hist['Low']
            volume = hist['Volume']
            
            # Moving averages
            ma_20 = close_prices.rolling(window=20).mean()
            ma_50 = close_prices.rolling(window=50).mean()
            
            # RSI
            rsi = self._calculate_rsi(close_prices)
            
            # MACD
            macd, macd_signal = self._calculate_macd(close_prices)
            
            # Bollinger Bands
            bb_upper, bb_lower = self._calculate_bollinger_bands(close_prices)
            
            # Volume analysis
            avg_volume = volume.rolling(window=20).mean()
            volume_ratio = volume.iloc[-1] / avg_volume.iloc[-1] if not avg_volume.empty else 1
            
            # Current price relative to moving averages
            current_price = close_prices.iloc[-1]
            ma_20_current = ma_20.iloc[-1] if not ma_20.empty else current_price
            ma_50_current = ma_50.iloc[-1] if not ma_50.empty else current_price
            
            # Technical scoring
            score_components = []
            
            # Price vs MA scoring
            if current_price > ma_20_current > ma_50_current:
                score_components.append(0.9)  # Strong uptrend
            elif current_price > ma_20_current:
                score_components.append(0.7)  # Mild uptrend
            elif current_price < ma_20_current < ma_50_current:
                score_components.append(0.1)  # Downtrend
            else:
                score_components.append(0.5)  # Sideways
            
            # RSI scoring
            if not rsi.empty:
                current_rsi = rsi.iloc[-1]
                if 40 <= current_rsi <= 60:
                    score_components.append(0.8)  # Neutral zone
                elif 30 <= current_rsi <= 40:
                    score_components.append(0.9)  # Oversold, potential buy
                elif current_rsi < 30:
                    score_components.append(0.7)  # Very oversold
                elif 60 <= current_rsi <= 70:
                    score_components.append(0.3)  # Overbought
                else:
                    score_components.append(0.1)  # Very overbought
            
            # Volume confirmation
            if volume_ratio > 1.5:
                score_components.append(0.8)  # High volume confirms trend
            elif volume_ratio > 1.2:
                score_components.append(0.7)
            else:
                score_components.append(0.5)
            
            technical_score = np.mean(score_components)
            
            return {
                'score': technical_score,
                'current_price': current_price,
                'ma_20': ma_20_current,
                'ma_50': ma_50_current,
                'rsi': rsi.iloc[-1] if not rsi.empty else 50,
                'macd': macd.iloc[-1] if not macd.empty else 0,
                'volume_ratio': volume_ratio,
                'price_change_1d': (current_price - close_prices.iloc[-2]) / close_prices.iloc[-2] if len(close_prices) > 1 else 0,
                'price_change_5d': (current_price - close_prices.iloc[-6]) / close_prices.iloc[-6] if len(close_prices) > 5 else 0,
                'volatility': close_prices.pct_change().std() * np.sqrt(252)  # Annualized volatility
            }
            
        except Exception as e:
            print(f"Error in technical analysis for {symbol}: {e}")
            return {'score': 0.5}
    
    def _calculate_rsi(self, prices: pd.Series, period: int = 14) -> pd.Series:
        """Calculate RSI"""
        delta = prices.diff()
        gain = (delta.where(delta > 0, 0)).rolling(window=period).mean()
        loss = (-delta.where(delta < 0, 0)).rolling(window=period).mean()
        rs = gain / loss
        rsi = 100 - (100 / (1 + rs))
        return rsi
    
    def _calculate_macd(self, prices: pd.Series) -> Tuple[pd.Series, pd.Series]:
        """Calculate MACD"""
        ema_12 = prices.ewm(span=12).mean()
        ema_26 = prices.ewm(span=26).mean()
        macd = ema_12 - ema_26
        signal = macd.ewm(span=9).mean()
        return macd, signal
    
    def _calculate_bollinger_bands(self, prices: pd.Series, period: int = 20) -> Tuple[pd.Series, pd.Series]:
        """Calculate Bollinger Bands"""
        sma = prices.rolling(window=period).mean()
        std = prices.rolling(window=period).std()
        upper_band = sma + (std * 2)
        lower_band = sma - (std * 2)
        return upper_band, lower_band
    
    async def _analyze_sentiment(self, symbol: str) -> Dict:
        """Analyze market sentiment for symbol"""
        try:
            # Get recent news
            stock = yf.Ticker(symbol)
            news = stock.news
            
            if not news:
                return {'score': 0.5, 'sentiment': 'neutral'}
            
            # Analyze sentiment of recent news
            recent_news = news[:10]  # Last 10 news items
            sentiments = []
            
            for item in recent_news:
                title = item.get('title', '')
                summary = item.get('summary', '')
                text = f"{title} {summary}"
                
                if text.strip():
                    sentiment_result = self.sentiment_analyzer(text[:512])  # Limit text length
                    if sentiment_result:
                        sentiment = sentiment_result[0]
                        if sentiment['label'] == 'POSITIVE':
                            sentiments.append(sentiment['score'])
                        elif sentiment['label'] == 'NEGATIVE':
                            sentiments.append(-sentiment['score'])
                        else:
                            sentiments.append(0)
            
            if sentiments:
                avg_sentiment = np.mean(sentiments)
                # Convert to 0-1 score where 0.5 is neutral
                sentiment_score = (avg_sentiment + 1) / 2
            else:
                sentiment_score = 0.5
            
            # Determine sentiment label
            if sentiment_score > 0.6:
                sentiment_label = 'positive'
            elif sentiment_score < 0.4:
                sentiment_label = 'negative'
            else:
                sentiment_label = 'neutral'
            
            return {
                'score': sentiment_score,
                'sentiment': sentiment_label,
                'news_count': len(recent_news),
                'confidence': min(len(sentiments) / 5, 1.0)  # More news = higher confidence
            }
            
        except Exception as e:
            print(f"Error in sentiment analysis for {symbol}: {e}")
            return {'score': 0.5, 'sentiment': 'neutral'}
    
    async def _predict_price_movement(self, symbol: str) -> Dict:
        """Predict price movement using LSTM model"""
        try:
            stock = yf.Ticker(symbol)
            hist = stock.history(period="1y", interval="1d")
            
            if len(hist) < 60:
                return {'prediction': 0, 'confidence': 0.5}
            
            # Prepare data for LSTM
            features = ['Open', 'High', 'Low', 'Close', 'Volume']
            data = hist[features].values
            
            # Normalize data
            from sklearn.preprocessing import MinMaxScaler
            scaler = MinMaxScaler(feature_range=(0, 1))
            scaled_data = scaler.fit_transform(data)
            
            # Create sequences
            sequence_length = 60
            X = []
            for i in range(sequence_length, len(scaled_data)):
                X.append(scaled_data[i-sequence_length:i])
            
            if len(X) == 0:
                return {'prediction': 0, 'confidence': 0.5}
            
            X = np.array(X)
            X = X.reshape((X.shape[0], X.shape[1], X.shape[2]))
            
            # Make prediction (simplified - in production would use trained model)
            # For demo, using simple trend analysis
            recent_prices = hist['Close'].tail(10).values
            price_trend = np.polyfit(range(len(recent_prices)), recent_prices, 1)[0]
            
            current_price = hist['Close'].iloc[-1]
            predicted_change = price_trend * 5  # 5-day prediction
            predicted_price = current_price + predicted_change
            
            price_change_percent = (predicted_price - current_price) / current_price
            
            return {
                'prediction': float(price_change_percent),
                'predicted_price': float(predicted_price),
                'current_price': float(current_price),
                'confidence': 0.7,  # Would be calculated from model uncertainty
                'time_horizon': '5_days'
            }
            
        except Exception as e:
            print(f"Error in price prediction for {symbol}: {e}")
            return {'prediction': 0, 'confidence': 0.5}
    
    async def _assess_risk(self, symbol: str, portfolio_context: Dict) -> Dict:
        """Assess risk factors for the symbol"""
        try:
            stock = yf.Ticker(symbol)
            hist = stock.history(period="1y", interval="1d")
            
            if hist.empty:
                return {'risk_score': 0.5}
            
            # Calculate volatility
            returns = hist['Close'].pct_change().dropna()
            volatility = returns.std() * np.sqrt(252)  # Annualized
            
            # Calculate beta (simplified - using market proxy)
            market_hist = yf.Ticker("^NSEI").history(period="1y", interval="1d")
            if not market_hist.empty:
                market_returns = market_hist['Close'].pct_change().dropna()
                # Align dates
                common_dates = returns.index.intersection(market_returns.index)
                if len(common_dates) > 20:
                    stock_returns_aligned = returns.loc[common_dates]
                    market_returns_aligned = market_returns.loc[common_dates]
                    beta = np.cov(stock_returns_aligned, market_returns_aligned)[0][1] / np.var(market_returns_aligned)
                else:
                    beta = 1.0
            else:
                beta = 1.0
            
            # Portfolio concentration risk
            current_holdings = portfolio_context.get('holdings', {})
            portfolio_value = portfolio_context.get('total_value', 1000000)
            current_position = current_holdings.get(symbol, 0)
            position_weight = current_position / portfolio_value
            
            # Risk score calculation
            volatility_score = min(volatility / 0.5, 1.0)  # Cap at 50% volatility
            beta_score = min(abs(beta - 1) / 1, 1.0)  # Distance from market beta
            concentration_score = min(position_weight / 0.1, 1.0)  # Cap at 10% position
            
            overall_risk_score = (volatility_score * 0.4 + beta_score * 0.3 + concentration_score * 0.3)
            
            return {
                'risk_score': overall_risk_score,
                'volatility': volatility,
                'beta': beta,
                'position_weight': position_weight,
                'risk_factors': self._identify_risk_factors(volatility, beta, position_weight)
            }
            
        except Exception as e:
            print(f"Error in risk assessment for {symbol}: {e}")
            return {'risk_score': 0.5}
    
    def _identify_risk_factors(self, volatility: float, beta: float, position_weight: float) -> List[str]:
        """Identify specific risk factors"""
        risk_factors = []
        
        if volatility > 0.3:
            risk_factors.append("High volatility")
        if beta > 1.5:
            risk_factors.append("High market sensitivity")
        if beta < 0.5:
            risk_factors.append("Low market correlation")
        if position_weight > 0.1:
            risk_factors.append("High portfolio concentration")
        
        return risk_factors
    
    async def _generate_reasoning(
        self,
        symbol: str,
        recommendation: TradingRecommendation,
        market_context: MarketContext
    ) -> str:
        """Generate natural language reasoning for recommendation"""
        
        prompt = f"""
        As an AI trading assistant, explain why I'm recommending to {recommendation.recommendation_type.value} {symbol}.
        
        Key factors:
        - Confidence: {recommendation.confidence_score:.2f}
        - Expected return: {recommendation.expected_return:.1%}
        - Risk level: {recommendation.risk_level}
        - Market sentiment: {market_context.overall_sentiment}
        - Supporting factors: {', '.join(recommendation.supporting_factors)}
        - Risk factors: {', '.join(recommendation.risk_factors)}
        
        Provide a clear, concise explanation in 2-3 sentences that a retail investor can understand.
        """
        
        try:
            response = await self.openai_client.chat.completions.acreate(
                model="gpt-3.5-turbo",
                messages=[{"role": "user", "content": prompt}],
                max_tokens=150,
                temperature=0.7
            )
            
            return response.choices[0].message.content.strip()
            
        except Exception as e:
            print(f"Error generating reasoning: {e}")
            return f"Based on our analysis, we recommend to {recommendation.recommendation_type.value} {symbol} with {recommendation.confidence_level.value} confidence."
    
    def _rank_recommendations(
        self,
        recommendations: List[TradingRecommendation],
        portfolio_context: Dict
    ) -> List[TradingRecommendation]:
        """Rank recommendations by overall score"""
        
        for rec in recommendations:
            # Calculate composite score
            score = (
                rec.confidence_score * 0.3 +
                rec.expected_return * 0.3 +
                rec.probability_of_success * 0.2 +
                (1 - self._risk_penalty(rec.risk_level)) * 0.2
            )
            rec.composite_score = score
        
        # Sort by composite score
        return sorted(recommendations, key=lambda x: x.composite_score, reverse=True)
    
    def _risk_penalty(self, risk_level: str) -> float:
        """Convert risk level to penalty factor"""
        risk_penalties = {
            'very_low': 0.0,
            'low': 0.1,
            'medium': 0.3,
            'high': 0.6,
            'very_high': 0.8
        }
        return risk_penalties.get(risk_level, 0.3)
```

### Recommendation Engine
```python
# Recommendation Engine Implementation
class RecommendationEngine:
    """Generate trading recommendations using ensemble approach"""
    
    def __init__(self):
        self.recommendation_weights = {
            'fundamental': 0.25,
            'technical': 0.25,
            'sentiment': 0.20,
            'price_prediction': 0.20,
            'risk_assessment': 0.10
        }
    
    async def generate_recommendation(
        self,
        symbol: str,
        fundamental_score: Dict,
        technical_score: Dict,
        sentiment_score: Dict,
        price_prediction: Dict,
        risk_assessment: Dict,
        market_context: MarketContext,
        personalization_factors: Dict,
        portfolio_context: Dict
    ) -> TradingRecommendation:
        """Generate comprehensive trading recommendation"""
        
        # Calculate weighted composite score
        scores = {
            'fundamental': fundamental_score.get('score', 0.5),
            'technical': technical_score.get('score', 0.5),
            'sentiment': sentiment_score.get('score', 0.5),
            'price_prediction': (price_prediction.get('prediction', 0) + 1) / 2,  # Normalize to 0-1
            'risk_assessment': 1 - risk_assessment.get('risk_score', 0.5)  # Lower risk = higher score
        }
        
        composite_score = sum(
            scores[factor] * weight 
            for factor, weight in self.recommendation_weights.items()
        )
        
        # Determine recommendation type
        prediction = price_prediction.get('prediction', 0)
        current_position = portfolio_context.get('holdings', {}).get(symbol, 0)
        
        if composite_score > 0.7 and prediction > 0.05:
            if current_position == 0:
                rec_type = RecommendationType.BUY
            else:
                rec_type = RecommendationType.INCREASE
        elif composite_score > 0.6 and prediction > 0.02:
            if current_position == 0:
                rec_type = RecommendationType.BUY
            else:
                rec_type = RecommendationType.HOLD
        elif composite_score < 0.3 or prediction < -0.05:
            if current_position > 0:
                rec_type = RecommendationType.SELL
            else:
                rec_type = RecommendationType.AVOID
        elif composite_score < 0.4 and current_position > 0:
            rec_type = RecommendationType.REDUCE
        else:
            rec_type = RecommendationType.HOLD
        
        # Determine confidence level
        confidence_score = composite_score
        if confidence_score > 0.8:
            confidence_level = ConfidenceLevel.VERY_HIGH
        elif confidence_score > 0.6:
            confidence_level = ConfidenceLevel.HIGH
        elif confidence_score > 0.4:
            confidence_level = ConfidenceLevel.MEDIUM
        elif confidence_score > 0.2:
            confidence_level = ConfidenceLevel.LOW
        else:
            confidence_level = ConfidenceLevel.VERY_LOW
        
        # Calculate targets
        current_price = technical_score.get('current_price', 0)
        predicted_price = price_prediction.get('predicted_price', current_price)
        
        if rec_type in [RecommendationType.BUY, RecommendationType.INCREASE]:
            target_price = predicted_price * 1.1  # 10% above prediction
            stop_loss = current_price * 0.95     # 5% stop loss
        elif rec_type == RecommendationType.SELL:
            target_price = current_price * 0.95  # Sell target
            stop_loss = None
        else:
            target_price = None
            stop_loss = None
        
        # Generate supporting factors
        supporting_factors = []
        risk_factors = []
        
        if fundamental_score.get('score', 0.5) > 0.6:
            supporting_factors.append("Strong fundamentals")
        if technical_score.get('score', 0.5) > 0.6:
            supporting_factors.append("Positive technical indicators")
        if sentiment_score.get('score', 0.5) > 0.6:
            supporting_factors.append("Positive market sentiment")
        if abs(prediction) > 0.03:
            supporting_factors.append(f"AI model predicts {prediction:.1%} price movement")
        
        if risk_assessment.get('risk_score', 0.5) > 0.6:
            risk_factors.append("High volatility")
        if risk_assessment.get('position_weight', 0) > 0.1:
            risk_factors.append("High portfolio concentration")
        
        # Personalize based on user factors
        risk_tolerance = personalization_factors.get('risk_tolerance', 'medium')
        if risk_tolerance == 'low' and confidence_level in [ConfidenceLevel.LOW, ConfidenceLevel.VERY_LOW]:
            rec_type = RecommendationType.AVOID
        
        return TradingRecommendation(
            symbol=symbol,
            recommendation_type=rec_type,
            confidence_level=confidence_level,
            confidence_score=confidence_score,
            target_price=target_price,
            stop_loss=stop_loss,
            time_horizon='medium',
            reasoning="",  # Will be generated separately
            risk_level=self._assess_overall_risk(risk_assessment),
            expected_return=prediction,
            probability_of_success=confidence_score,
            supporting_factors=supporting_factors,
            risk_factors=risk_factors,
            market_context=market_context.__dict__,
            personalization_factors=personalization_factors,
            timestamp=datetime.now()
        )
    
    def _assess_overall_risk(self, risk_assessment: Dict) -> str:
        """Assess overall risk level"""
        risk_score = risk_assessment.get('risk_score', 0.5)
        
        if risk_score > 0.8:
            return 'very_high'
        elif risk_score > 0.6:
            return 'high'
        elif risk_score > 0.4:
            return 'medium'
        elif risk_score > 0.2:
            return 'low'
        else:
            return 'very_low'

class MarketContextAnalyzer:
    """Analyze overall market context"""
    
    async def analyze_market_context(self, symbols: List[str]) -> MarketContext:
        """Analyze current market context"""
        
        # Get market indices
        nifty = yf.Ticker("^NSEI")
        nifty_hist = nifty.history(period="1mo", interval="1d")
        
        # Calculate market trend
        if not nifty_hist.empty:
            recent_prices = nifty_hist['Close'].tail(10)
            price_trend = np.polyfit(range(len(recent_prices)), recent_prices, 1)[0]
            
            if price_trend > 50:
                market_trend = 'bullish'
            elif price_trend < -50:
                market_trend = 'bearish'
            else:
                market_trend = 'sideways'
        else:
            market_trend = 'sideways'
        
        # Calculate volatility regime
        if not nifty_hist.empty:
            returns = nifty_hist['Close'].pct_change().dropna()
            volatility = returns.std() * np.sqrt(252)
            
            if volatility > 0.25:
                volatility_regime = 'high'
            elif volatility > 0.15:
                volatility_regime = 'medium'
            else:
                volatility_regime = 'low'
        else:
            volatility_regime = 'medium'
        
        return MarketContext(
            overall_sentiment=0.5,  # Would be calculated from multiple sources
            sector_sentiment={},    # Would be calculated per sector
            volatility_regime=volatility_regime,
            market_trend=market_trend,
            economic_indicators={}, # Would include inflation, interest rates, etc.
            news_sentiment=0.5,     # Would be calculated from news analysis
            technical_indicators={} # Would include market-wide technical indicators
        )

class PersonalizationEngine:
    """Personalize recommendations based on user profile"""
    
    async def get_user_factors(self, user_id: str) -> Dict:
        """Get user-specific factors for personalization"""
        # In production, this would fetch from user profile and behavioral analysis
        return {
            'risk_tolerance': 'medium',
            'investment_horizon': 'medium',
            'experience_level': 'intermediate',
            'preferred_sectors': ['technology', 'healthcare'],
            'max_position_size': 0.1,
            'behavioral_biases': ['loss_aversion'],
            'trading_style': 'swing_trader'
        }
```

## Acceptance Criteria

### AI Recommendation Quality
- [ ] **Accuracy**: Achieve 70%+ accuracy in recommendation outcomes over 3-month period
- [ ] **Personalization**: Recommendations adapt to individual user risk tolerance and behavior
- [ ] **Multi-factor Analysis**: Integrate fundamental, technical, sentiment, and ML predictions
- [ ] **Confidence Scoring**: Provide accurate confidence levels with calibrated probabilities

### User Experience
- [ ] **Response Time**: Generate recommendations in <30 seconds
- [ ] **Natural Language**: Clear, understandable reasoning for each recommendation
- [ ] **Interactive Assistant**: Support conversational queries about recommendations
- [ ] **Visual Analytics**: Rich charts and visualizations supporting recommendations

### Performance & Scalability  
- [ ] **Real-time Updates**: Update recommendations as market conditions change
- [ ] **Concurrent Users**: Support 10K+ concurrent users requesting recommendations
- [ ] **API Performance**: <500ms response time for recommendation API
- [ ] **Model Performance**: Retrain models weekly with fresh data

### Business Intelligence
- [ ] **A/B Testing**: Framework for testing recommendation algorithm improvements
- [ ] **Success Tracking**: Track recommendation performance and user outcomes
- [ ] **Revenue Attribution**: Track premium subscriptions driven by AI recommendations
- [ ] **User Engagement**: Monitor increased trading activity from AI recommendations

## Testing Strategy

### Algorithm Validation
- Backtesting recommendation performance against historical data
- Cross-validation of ML models with out-of-sample data
- A/B testing of recommendation algorithms
- Comparison against benchmark strategies

### User Experience Testing
- Usability testing of AI assistant interface
- Natural language processing accuracy validation
- Response time and performance testing
- Mobile experience optimization

### Production Testing
- Load testing for concurrent recommendation requests  
- Integration testing with trading platform
- Real-time data feed integration validation
- Error handling and fallback mechanism testing

### Business Metrics Testing
- Recommendation conversion rate measurement
- User engagement and retention impact analysis
- Revenue attribution from AI-driven trades
- Performance impact on user portfolios

## Definition of Done
- [ ] Multi-factor recommendation engine operational with 70%+ accuracy
- [ ] Natural language reasoning generation for all recommendations
- [ ] Real-time recommendation updates based on market changes
- [ ] Personalized recommendations based on user behavioral profiles  
- [ ] Interactive AI assistant for trading queries implemented
- [ ] A/B testing framework for continuous algorithm improvement
- [ ] Performance monitoring and success tracking dashboard
- [ ] Integration testing with existing trading platform completed
- [ ] Premium feature access control and usage tracking operational
- [ ] User documentation and onboarding for AI assistant features

## Story Points: 31

## Dependencies
- AI-001: ML Infrastructure & Pipeline Setup
- AI-002: Behavioral Pattern Recognition System
- Market data feed integration
- User behavioral analytics system

## Notes
- Consider regulatory compliance for AI-driven investment advice
- Implement clear disclaimers about AI recommendation limitations
- Regular model validation and performance monitoring essential
- Integration with risk management systems for position sizing recommendations