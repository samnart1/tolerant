#!/bin/bash
# Setup Kubernetes cluster and infrastructure for thesis experiments

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "🚀 Setting up Kubernetes cluster for thesis experiments"
echo "=================================================="

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"
    
    local missing_tools=()
    
    command -v kubectl >/dev/null 2>&1 || missing_tools+=("kubectl")
    command -v docker >/dev/null 2>&1 || missing_tools+=("docker")
    command -v kind >/dev/null 2>&1 || missing_tools+=("kind")
    command -v helm >/dev/null 2>&1 || missing_tools+=("helm")
    
    if [ ${#missing_tools[@]} -ne 0 ]; then
        echo -e "${RED}Error: Missing required tools: ${missing_tools[*]}${NC}"
        echo "Please install the missing tools and try again."
        exit 1
    fi
    
    echo -e "${GREEN}✓ All prerequisites met${NC}"
}

# Create kind cluster
create_cluster() {
    echo -e "${YELLOW}Creating kind cluster...${NC}"
    
    if kind get clusters | grep -q "thesis-cluster"; then
        echo "Cluster 'thesis-cluster' already exists. Deleting..."
        kind delete cluster --name thesis-cluster
    fi
    
    cat <<EOF | kind create cluster --name thesis-cluster --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    kubeadmConfigPatches:
    - |
      kind: InitConfiguration
      nodeRegistration:
        kubeletExtraArgs:
          node-labels: "ingress-ready=true"
    extraPortMappings:
    - containerPort: 80
      hostPort: 80
      protocol: TCP
    - containerPort: 443
      hostPort: 443
      protocol: TCP
  - role: worker
  - role: worker
  - role: worker
EOF
    
    echo -e "${GREEN}✓ Cluster created${NC}"
}

# Install Istio
install_istio() {
    echo -e "${YELLOW}Installing Istio service mesh...${NC}"
    
    # Download Istio
    if [ ! -d "$HOME/istio-1.20.0" ]; then
        curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.20.0 sh -
        sudo mv istio-1.20.0 $HOME/
    fi
    
    export PATH=$HOME/istio-1.20.0/bin:$PATH
    
    # Install Istio with demo profile
    istioctl install --set profile=demo -y
    
    # Enable automatic sidecar injection
    kubectl label namespace default istio-injection=enabled --overwrite
    
    echo -e "${GREEN}✓ Istio installed${NC}"
}

# Install Prometheus and Grafana
install_monitoring() {
    echo -e "${YELLOW}Installing monitoring stack...${NC}"
    
    # Add Helm repos
    helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
    helm repo add grafana https://grafana.github.io/helm-charts
    helm repo update
    
    # Install Prometheus
    helm upgrade --install prometheus prometheus-community/prometheus \
        --namespace monitoring --create-namespace \
        --set server.persistentVolume.enabled=false \
        --set alertmanager.persistentVolume.enabled=false \
        --wait
    
    # Install Grafana
    helm upgrade --install grafana grafana/grafana \
        --namespace monitoring \
        --set persistence.enabled=false \
        --set adminPassword=admin \
        --wait
    
    # Install Jaeger for distributed tracing
    kubectl apply -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/main/deploy/crds/jaegertracing.io_jaegers_crd.yaml
    kubectl apply -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/main/deploy/service_account.yaml
    kubectl apply -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/main/deploy/role.yaml
    kubectl apply -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/main/deploy/role_binding.yaml
    kubectl apply -f https://raw.githubusercontent.com/jaegertracing/jaeger-operator/main/deploy/operator.yaml
    
    echo -e "${GREEN}✓ Monitoring stack installed${NC}"
}

# Install Chaos Mesh
install_chaos_mesh() {
    echo -e "${YELLOW}Installing Chaos Mesh...${NC}"
    
    helm repo add chaos-mesh https://charts.chaos-mesh.org
    helm repo update
    
    helm upgrade --install chaos-mesh chaos-mesh/chaos-mesh \
        --namespace chaos-mesh --create-namespace \
        --set chaosDaemon.runtime=containerd \
        --set chaosDaemon.socketPath=/run/containerd/containerd.sock \
        --set dashboard.create=true \
        --wait
    
    echo -e "${GREEN}✓ Chaos Mesh installed${NC}"
}

# Install MetalLB for LoadBalancer support
install_metallb() {
    echo -e "${YELLOW}Installing MetalLB...${NC}"
    
    kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.13.12/config/manifests/metallb-native.yaml
    
    # Wait for MetalLB to be ready
    kubectl wait --namespace metallb-system \
        --for=condition=ready pod \
        --selector=app=metallb \
        --timeout=90s
    
    # Configure MetalLB IP address pool
    docker network inspect -f '{{.IPAM.Config}}' kind | grep -oP '\d+\.\d+\.\d+' | head -n1 > /tmp/kind_network
    SUBNET=$(cat /tmp/kind_network)
    
    cat <<EOF | kubectl apply -f -
apiVersion: metallb.io/v1beta1
kind: IPAddressPool
metadata:
  name: example
  namespace: metallb-system
spec:
  addresses:
  - ${SUBNET}.200-${SUBNET}.250
---
apiVersion: metallb.io/v1beta1
kind: L2Advertisement
metadata:
  name: empty
  namespace: metallb-system
EOF
    
    echo -e "${GREEN}✓ MetalLB installed${NC}"
}

# Deploy infrastructure components
deploy_infrastructure() {
    echo -e "${YELLOW}Deploying infrastructure components...${NC}"
    
    cd "$PROJECT_ROOT"
    
    # Apply Istio configuration
    kubectl apply -f infrastructure/istio/
    
    # Apply monitoring configuration
    kubectl apply -f infrastructure/monitoring/prometheus/
    
    # Deploy RabbitMQ
    kubectl apply -f infrastructure/kubernetes/base/rabbitmq.yaml
    
    # Wait for RabbitMQ to be ready
    kubectl wait --for=condition=ready pod -l app=rabbitmq --timeout=300s
    
    echo -e "${GREEN}✓ Infrastructure components deployed${NC}"
}

# Import Grafana dashboards
setup_grafana() {
    echo -e "${YELLOW}Setting up Grafana dashboards...${NC}"
    
    # Port forward Grafana
    kubectl port-forward -n monitoring svc/grafana 3000:80 &
    GRAFANA_PID=$!
    
    sleep 5
    
    # Create datasource
    curl -X POST http://admin:admin@localhost:3000/api/datasources \
        -H "Content-Type: application/json" \
        -d '{
            "name": "Prometheus",
            "type": "prometheus",
            "url": "http://prometheus-server.monitoring.svc.cluster.local",
            "access": "proxy",
            "isDefault": true
        }' 2>/dev/null || true
    
    # Import dashboard
    if [ -f "$PROJECT_ROOT/infrastructure/monitoring/grafana/dashboards/overview.json" ]; then
        curl -X POST http://admin:admin@localhost:3000/api/dashboards/db \
            -H "Content-Type: application/json" \
            -d @"$PROJECT_ROOT/infrastructure/monitoring/grafana/dashboards/overview.json" \
            2>/dev/null || true
    fi
    
    kill $GRAFANA_PID 2>/dev/null || true
    
    echo -e "${GREEN}✓ Grafana configured${NC}"
}

# Print access information
print_access_info() {
    echo ""
    echo "=================================================="
    echo -e "${GREEN}Cluster setup complete!${NC}"
    echo "=================================================="
    echo ""
    echo "Access URLs (use kubectl port-forward):"
    echo ""
    echo "  Grafana:"
    echo "    kubectl port-forward -n monitoring svc/grafana 3000:80"
    echo "    URL: http://localhost:3000"
    echo "    User: admin / Password: admin"
    echo ""
    echo "  Prometheus:"
    echo "    kubectl port-forward -n monitoring svc/prometheus-server 9090:80"
    echo "    URL: http://localhost:9090"
    echo ""
    echo "  Chaos Mesh Dashboard:"
    echo "    kubectl port-forward -n chaos-mesh svc/chaos-dashboard 2333:2333"
    echo "    URL: http://localhost:2333"
    echo ""
    echo "  RabbitMQ Management:"
    echo "    kubectl port-forward svc/rabbitmq 15672:15672"
    echo "    URL: http://localhost:15672"
    echo "    User: admin / Password: admin"
    echo ""
    echo "Next steps:"
    echo "  1. Build services: make build-all"
    echo "  2. Deploy services: make deploy-all"
    echo "  3. Run experiments: make experiment-baseline"
    echo ""
}

# Main execution
main() {
    check_prerequisites
    create_cluster
    install_istio
    install_monitoring
    install_chaos_mesh
    install_metallb
    deploy_infrastructure
    setup_grafana
    print_access_info
}

main