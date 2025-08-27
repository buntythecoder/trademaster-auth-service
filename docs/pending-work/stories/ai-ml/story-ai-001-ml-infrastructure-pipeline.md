# Story AI-001: ML Infrastructure & Pipeline Setup

## Epic
Epic 3: AI Integration & Behavioral Analytics

## Story Overview
**As a** TradeMaster AI engineering team  
**I want** a complete MLOps infrastructure and pipeline  
**So that** we can train, deploy, and monitor ML models for trading insights at scale

## Business Value
- **Competitive Advantage**: AI-powered trading insights differentiate from traditional platforms
- **Scalability**: Support for 100K+ concurrent users with real-time ML predictions
- **Model Reliability**: 99.5% uptime for AI services with automated failover
- **Rapid Innovation**: Deploy new ML models in <24 hours vs weeks

## Technical Requirements

### MLOps Architecture
```python
# MLOps Configuration and Infrastructure
from dataclasses import dataclass
from typing import Dict, List, Optional, Union
import mlflow
import kubernetes
from airflow import DAG
from airflow.operators import PythonOperator
import docker

@dataclass
class MLInfrastructureConfig:
    """Configuration for ML infrastructure components"""
    
    # Model Registry
    mlflow_tracking_uri: str = "http://mlflow-server:5000"
    model_registry_uri: str = "mysql://mlflow:password@mysql:3306/mlflow"
    
    # Feature Store
    feast_repo_path: str = "/opt/feast"
    feature_store_uri: str = "redis://redis:6379/0"
    offline_store_uri: str = "postgresql://feast:password@postgres:5432/feast"
    
    # Model Serving
    model_serving_replicas: int = 3
    serving_memory_limit: str = "2Gi"
    serving_cpu_limit: str = "1000m"
    
    # Monitoring
    prometheus_endpoint: str = "http://prometheus:9090"
    grafana_endpoint: str = "http://grafana:3000"
    
    # Data Pipeline
    airflow_webserver_uri: str = "http://airflow-webserver:8080"
    spark_master_uri: str = "spark://spark-master:7077"
    
    # Kubernetes
    kubernetes_namespace: str = "trademaster-ml"
    model_serving_service_name: str = "ml-model-server"

class MLPipelineOrchestrator:
    """Main orchestrator for ML operations"""
    
    def __init__(self, config: MLInfrastructureConfig):
        self.config = config
        self.mlflow_client = mlflow.tracking.MlflowClient(config.mlflow_tracking_uri)
        self.k8s_client = kubernetes.client.ApiClient()
        
    def setup_model_registry(self) -> None:
        """Initialize MLflow model registry"""
        mlflow.set_tracking_uri(self.config.mlflow_tracking_uri)
        mlflow.set_registry_uri(self.config.model_registry_uri)
        
        # Create registered model names
        model_names = [
            "behavioral-pattern-classifier",
            "price-prediction-lstm",
            "sentiment-analyzer",
            "risk-assessment-model",
            "portfolio-optimizer"
        ]
        
        for model_name in model_names:
            try:
                self.mlflow_client.create_registered_model(model_name)
            except mlflow.exceptions.RestException:
                pass  # Model already exists
    
    def setup_feature_store(self) -> None:
        """Initialize Feast feature store"""
        from feast import FeatureStore, Entity, FeatureView, Field
        from feast.types import Float64, Int64, String
        
        # Initialize feature store
        fs = FeatureStore(repo_path=self.config.feast_repo_path)
        
        # Define entities
        user_entity = Entity(
            name="user_id",
            value_type=String,
            description="User identifier"
        )
        
        stock_entity = Entity(
            name="symbol",
            value_type=String,
            description="Stock symbol"
        )
        
        # Define feature views
        user_behavior_features = FeatureView(
            name="user_behavior_features",
            entities=[user_entity],
            schema=[
                Field(name="avg_trade_size", dtype=Float64),
                Field(name="trade_frequency", dtype=Float64),
                Field(name="risk_score", dtype=Float64),
                Field(name="portfolio_diversity", dtype=Float64),
            ],
            online=True,
            batch_source=None,  # Would be configured with actual data source
            ttl=86400,  # 24 hours
        )
        
        market_features = FeatureView(
            name="market_features",
            entities=[stock_entity],
            schema=[
                Field(name="price_volatility", dtype=Float64),
                Field(name="volume_ma_20", dtype=Float64),
                Field(name="rsi", dtype=Float64),
                Field(name="sentiment_score", dtype=Float64),
            ],
            online=True,
            batch_source=None,
            ttl=3600,  # 1 hour
        )
        
        # Apply feature store configuration
        fs.apply([user_entity, stock_entity, user_behavior_features, market_features])
    
    def create_training_pipeline(self) -> DAG:
        """Create Airflow DAG for model training pipeline"""
        from datetime import datetime, timedelta
        
        default_args = {
            'owner': 'ml-team',
            'depends_on_past': False,
            'start_date': datetime(2024, 1, 1),
            'email_on_failure': True,
            'email_on_retry': False,
            'retries': 2,
            'retry_delay': timedelta(minutes=5),
        }
        
        dag = DAG(
            'ml_training_pipeline',
            default_args=default_args,
            description='ML model training and deployment pipeline',
            schedule_interval='@daily',
            catchup=False,
        )
        
        # Data extraction task
        extract_data = PythonOperator(
            task_id='extract_training_data',
            python_callable=self._extract_training_data,
            dag=dag,
        )
        
        # Feature engineering task
        engineer_features = PythonOperator(
            task_id='engineer_features',
            python_callable=self._engineer_features,
            dag=dag,
        )
        
        # Model training tasks (parallel)
        train_behavioral_model = PythonOperator(
            task_id='train_behavioral_model',
            python_callable=self._train_behavioral_model,
            dag=dag,
        )
        
        train_price_model = PythonOperator(
            task_id='train_price_model',
            python_callable=self._train_price_prediction_model,
            dag=dag,
        )
        
        # Model validation task
        validate_models = PythonOperator(
            task_id='validate_models',
            python_callable=self._validate_models,
            dag=dag,
        )
        
        # Model deployment task
        deploy_models = PythonOperator(
            task_id='deploy_models',
            python_callable=self._deploy_models,
            dag=dag,
        )
        
        # Set task dependencies
        extract_data >> engineer_features
        engineer_features >> [train_behavioral_model, train_price_model]
        [train_behavioral_model, train_price_model] >> validate_models
        validate_models >> deploy_models
        
        return dag
    
    def _extract_training_data(self, **context) -> None:
        """Extract data for model training"""
        import pandas as pd
        from sqlalchemy import create_engine
        
        # Connect to data warehouse
        engine = create_engine("postgresql://user:pass@warehouse:5432/trademaster")
        
        # Extract user behavior data
        user_behavior_query = """
        SELECT 
            user_id,
            DATE(created_at) as date,
            COUNT(*) as trade_count,
            AVG(amount) as avg_trade_size,
            SUM(amount) as total_volume,
            STDDEV(amount) as trade_size_volatility,
            COUNT(DISTINCT symbol) as stocks_traded
        FROM trades 
        WHERE created_at >= NOW() - INTERVAL '90 days'
        GROUP BY user_id, DATE(created_at)
        """
        
        user_behavior_df = pd.read_sql(user_behavior_query, engine)
        
        # Extract market data
        market_data_query = """
        SELECT 
            symbol,
            DATE(timestamp) as date,
            price,
            volume,
            high,
            low,
            close
        FROM market_data 
        WHERE timestamp >= NOW() - INTERVAL '90 days'
        ORDER BY symbol, timestamp
        """
        
        market_data_df = pd.read_sql(market_data_query, engine)
        
        # Store in feature store
        self._store_features(user_behavior_df, market_data_df)
    
    def _engineer_features(self, **context) -> None:
        """Feature engineering for ML models"""
        import pandas as pd
        import numpy as np
        from feast import FeatureStore
        
        fs = FeatureStore(repo_path=self.config.feast_repo_path)
        
        # Get historical features
        entity_df = pd.DataFrame({
            "user_id": ["user_1", "user_2", "user_3"],  # Example users
            "symbol": ["AAPL", "GOOGL", "MSFT"],        # Example symbols
            "event_timestamp": [pd.Timestamp.now()] * 3
        })
        
        training_df = fs.get_historical_features(
            entity_df=entity_df,
            features=[
                "user_behavior_features:avg_trade_size",
                "user_behavior_features:trade_frequency", 
                "user_behavior_features:risk_score",
                "market_features:price_volatility",
                "market_features:volume_ma_20",
                "market_features:rsi"
            ],
        ).to_df()
        
        # Additional feature engineering
        training_df['price_momentum'] = training_df['price_volatility'].rolling(5).mean()
        training_df['volume_trend'] = training_df['volume_ma_20'].pct_change()
        training_df['risk_adjusted_size'] = training_df['avg_trade_size'] / (training_df['risk_score'] + 1)
        
        # Store engineered features
        training_df.to_parquet('/tmp/engineered_features.parquet')
    
    def create_model_serving_deployment(self) -> Dict:
        """Create Kubernetes deployment for model serving"""
        deployment_spec = {
            "apiVersion": "apps/v1",
            "kind": "Deployment",
            "metadata": {
                "name": "ml-model-server",
                "namespace": self.config.kubernetes_namespace,
                "labels": {
                    "app": "ml-model-server",
                    "version": "v1"
                }
            },
            "spec": {
                "replicas": self.config.model_serving_replicas,
                "selector": {
                    "matchLabels": {
                        "app": "ml-model-server"
                    }
                },
                "template": {
                    "metadata": {
                        "labels": {
                            "app": "ml-model-server"
                        }
                    },
                    "spec": {
                        "containers": [{
                            "name": "model-server",
                            "image": "trademaster/ml-model-server:latest",
                            "ports": [{"containerPort": 8080}],
                            "env": [
                                {
                                    "name": "MLFLOW_TRACKING_URI",
                                    "value": self.config.mlflow_tracking_uri
                                },
                                {
                                    "name": "FEAST_REPO_PATH",
                                    "value": self.config.feast_repo_path
                                }
                            ],
                            "resources": {
                                "limits": {
                                    "memory": self.config.serving_memory_limit,
                                    "cpu": self.config.serving_cpu_limit
                                },
                                "requests": {
                                    "memory": "1Gi",
                                    "cpu": "500m"
                                }
                            },
                            "livenessProbe": {
                                "httpGet": {
                                    "path": "/health",
                                    "port": 8080
                                },
                                "initialDelaySeconds": 30,
                                "periodSeconds": 10
                            },
                            "readinessProbe": {
                                "httpGet": {
                                    "path": "/ready",
                                    "port": 8080
                                },
                                "initialDelaySeconds": 5,
                                "periodSeconds": 5
                            }
                        }],
                        "imagePullSecrets": [{"name": "regcred"}]
                    }
                }
            }
        }
        
        return deployment_spec
```

### Model Serving Infrastructure
```python
# FastAPI-based Model Serving Service
from fastapi import FastAPI, HTTPException, Depends, BackgroundTasks
from pydantic import BaseModel
from typing import Dict, List, Optional
import mlflow.pyfunc
import numpy as np
import pandas as pd
from feast import FeatureStore
import redis
import asyncio
import logging
from prometheus_client import Counter, Histogram, generate_latest

# Metrics
prediction_requests = Counter('ml_prediction_requests_total', 'Total prediction requests', ['model_name'])
prediction_latency = Histogram('ml_prediction_duration_seconds', 'Prediction latency', ['model_name'])
prediction_errors = Counter('ml_prediction_errors_total', 'Total prediction errors', ['model_name', 'error_type'])

class PredictionRequest(BaseModel):
    user_id: str
    symbol: str
    features: Optional[Dict] = None
    model_name: str

class PredictionResponse(BaseModel):
    prediction: float
    confidence: float
    model_version: str
    timestamp: str
    feature_importance: Optional[Dict] = None

class ModelServingService:
    """High-performance model serving service"""
    
    def __init__(self):
        self.app = FastAPI(title="TradeMaster ML API", version="1.0.0")
        self.models = {}
        self.feature_store = FeatureStore(repo_path="/opt/feast")
        self.redis_client = redis.Redis(host='redis', port=6379, db=0)
        self.setup_routes()
        self.load_models()
    
    def setup_routes(self):
        """Setup FastAPI routes"""
        
        @self.app.get("/health")
        async def health_check():
            return {"status": "healthy", "models_loaded": len(self.models)}
        
        @self.app.get("/ready")
        async def readiness_check():
            if len(self.models) == 0:
                raise HTTPException(status_code=503, detail="No models loaded")
            return {"status": "ready", "models": list(self.models.keys())}
        
        @self.app.post("/predict", response_model=PredictionResponse)
        async def predict(request: PredictionRequest):
            return await self.make_prediction(request)
        
        @self.app.post("/batch_predict")
        async def batch_predict(requests: List[PredictionRequest]):
            tasks = [self.make_prediction(req) for req in requests]
            results = await asyncio.gather(*tasks)
            return {"predictions": results}
        
        @self.app.get("/metrics")
        async def metrics():
            return generate_latest()
        
        @self.app.get("/models")
        async def list_models():
            return {
                name: {
                    "version": model["version"],
                    "loaded_at": model["loaded_at"]
                }
                for name, model in self.models.items()
            }
    
    def load_models(self):
        """Load ML models from MLflow registry"""
        mlflow.set_tracking_uri("http://mlflow-server:5000")
        
        model_configs = [
            {"name": "behavioral-pattern-classifier", "stage": "Production"},
            {"name": "price-prediction-lstm", "stage": "Production"},
            {"name": "sentiment-analyzer", "stage": "Production"},
            {"name": "risk-assessment-model", "stage": "Production"},
        ]
        
        for config in model_configs:
            try:
                model_uri = f"models:/{config['name']}/{config['stage']}"
                model = mlflow.pyfunc.load_model(model_uri)
                
                self.models[config['name']] = {
                    "model": model,
                    "version": self._get_model_version(config['name'], config['stage']),
                    "loaded_at": pd.Timestamp.now().isoformat()
                }
                
                logging.info(f"Loaded model: {config['name']}")
                
            except Exception as e:
                logging.error(f"Failed to load model {config['name']}: {e}")
    
    async def make_prediction(self, request: PredictionRequest) -> PredictionResponse:
        """Make ML prediction with feature enrichment"""
        with prediction_latency.labels(model_name=request.model_name).time():
            prediction_requests.labels(model_name=request.model_name).inc()
            
            try:
                # Check cache first
                cache_key = f"prediction:{request.model_name}:{request.user_id}:{request.symbol}"
                cached_result = self.redis_client.get(cache_key)
                
                if cached_result:
                    return PredictionResponse.parse_raw(cached_result)
                
                # Get model
                if request.model_name not in self.models:
                    raise HTTPException(status_code=404, detail=f"Model {request.model_name} not found")
                
                model_info = self.models[request.model_name]
                
                # Get features
                features_df = await self._get_features(request.user_id, request.symbol, request.features)
                
                # Make prediction
                prediction = model_info["model"].predict(features_df)[0]
                
                # Calculate confidence (simplified)
                confidence = 0.85  # Would be calculated from model uncertainty
                
                response = PredictionResponse(
                    prediction=float(prediction),
                    confidence=confidence,
                    model_version=model_info["version"],
                    timestamp=pd.Timestamp.now().isoformat()
                )
                
                # Cache result for 5 minutes
                self.redis_client.setex(
                    cache_key,
                    300,
                    response.json()
                )
                
                return response
                
            except Exception as e:
                prediction_errors.labels(
                    model_name=request.model_name,
                    error_type=type(e).__name__
                ).inc()
                
                logging.error(f"Prediction error: {e}")
                raise HTTPException(status_code=500, detail=str(e))
    
    async def _get_features(self, user_id: str, symbol: str, extra_features: Optional[Dict]) -> pd.DataFrame:
        """Get features from feature store"""
        entity_df = pd.DataFrame({
            "user_id": [user_id],
            "symbol": [symbol],
            "event_timestamp": [pd.Timestamp.now()]
        })
        
        # Get features from Feast
        feature_vector = self.feature_store.get_online_features(
            features=[
                "user_behavior_features:avg_trade_size",
                "user_behavior_features:trade_frequency",
                "user_behavior_features:risk_score",
                "market_features:price_volatility",
                "market_features:volume_ma_20",
                "market_features:rsi"
            ],
            entity_rows=[{"user_id": user_id, "symbol": symbol}]
        ).to_dict()
        
        # Convert to DataFrame
        features_df = pd.DataFrame([feature_vector])
        
        # Add extra features if provided
        if extra_features:
            for key, value in extra_features.items():
                features_df[key] = value
        
        return features_df
    
    def _get_model_version(self, name: str, stage: str) -> str:
        """Get model version from MLflow"""
        client = mlflow.tracking.MlflowClient()
        model_version = client.get_latest_versions(name, stages=[stage])[0]
        return model_version.version
```

### Monitoring and Observability
```yaml
# Prometheus Configuration for ML Monitoring
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "ml_alerts.yml"

scrape_configs:
  - job_name: 'ml-model-server'
    static_configs:
      - targets: ['ml-model-server:8080']
    metrics_path: /metrics
    scrape_interval: 5s
    
  - job_name: 'mlflow'
    static_configs:
      - targets: ['mlflow-server:5000']
    scrape_interval: 30s
    
  - job_name: 'feast'
    static_configs:
      - targets: ['feast-server:6566']
    scrape_interval: 30s

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

---
# ML-specific Alerting Rules
groups:
- name: ml_model_alerts
  rules:
  - alert: ModelPredictionLatencyHigh
    expr: histogram_quantile(0.95, ml_prediction_duration_seconds_bucket) > 1.0
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High prediction latency detected"
      description: "95th percentile prediction latency is {{ $value }}s"
      
  - alert: ModelErrorRateHigh
    expr: rate(ml_prediction_errors_total[5m]) > 0.1
    for: 2m
    labels:
      severity: critical
    annotations:
      summary: "High model error rate"
      description: "Error rate is {{ $value }} errors per second"
      
  - alert: ModelServerDown
    expr: up{job="ml-model-server"} == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "ML model server is down"
      description: "Model server has been down for more than 1 minute"
      
  - alert: FeatureStoreLatencyHigh
    expr: feast_feature_retrieval_duration_seconds > 0.5
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Feature store latency is high"
      description: "Feature retrieval taking {{ $value }}s"
```

## Acceptance Criteria

### Infrastructure Setup
- [ ] **MLflow Registry**: Model versioning and registry operational
- [ ] **Feature Store**: Feast feature store with online/offline serving
- [ ] **Model Serving**: Kubernetes-based model serving with autoscaling
- [ ] **Pipeline Orchestration**: Airflow DAGs for automated model training

### Performance Requirements
- [ ] **Prediction Latency**: <100ms for single predictions, <500ms for batch
- [ ] **Throughput**: Support 10K+ predictions per minute
- [ ] **Availability**: 99.9% uptime for model serving endpoints
- [ ] **Scalability**: Auto-scale from 3 to 20 replicas based on load

### Data Pipeline
- [ ] **Automated Training**: Daily model retraining with fresh data
- [ ] **Feature Engineering**: Automated feature computation and storage
- [ ] **Data Validation**: Data quality checks and drift detection
- [ ] **Model Validation**: Automated model performance validation

### Monitoring & Observability
- [ ] **Model Metrics**: Prediction latency, throughput, error rates
- [ ] **Data Metrics**: Feature drift, data quality, freshness
- [ ] **Infrastructure Metrics**: Resource utilization, scaling events
- [ ] **Business Metrics**: Model accuracy, business impact tracking

## Testing Strategy

### Infrastructure Testing
- Kubernetes cluster resilience testing
- Model serving load testing
- Feature store performance validation
- Pipeline failure and recovery testing

### ML Pipeline Testing
- End-to-end pipeline validation
- Model training automation testing
- Feature engineering correctness
- Model deployment automation

### Performance Testing
- Prediction latency benchmarking
- Concurrent request handling
- Memory and CPU optimization
- Auto-scaling behavior validation

### Integration Testing
- MLflow model registry integration
- Feast feature store integration
- Monitoring system integration
- API endpoint functionality

## Definition of Done
- [ ] Complete MLOps infrastructure deployed on Kubernetes
- [ ] MLflow model registry with version management operational
- [ ] Feast feature store serving online and offline features
- [ ] Automated training pipeline with Airflow DAGs running
- [ ] Model serving API with <100ms response times
- [ ] Comprehensive monitoring with Prometheus and Grafana
- [ ] Auto-scaling model serving based on traffic patterns
- [ ] CI/CD pipeline for model deployment implemented
- [ ] Security scanning and compliance validation completed
- [ ] Documentation for ML infrastructure and operations

## Story Points: 34

## Dependencies
- Kubernetes cluster setup and configuration
- Database infrastructure for MLflow and Feast
- Container registry for model serving images
- Monitoring infrastructure (Prometheus, Grafana)

## Notes
- Consider multi-region deployment for disaster recovery
- Implement model A/B testing framework for gradual rollouts
- Regular backup strategy for model registry and feature store
- Integration with existing authentication and authorization systems