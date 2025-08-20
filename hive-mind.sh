# Enterprise Hive Mind Orchestrator Script
set -e

STORY_FILE=$1
PROJECT_NAME=$2

if [ -z "$STORY_FILE" ] || [ -z "$PROJECT_NAME" ]; then
    echo "Usage: ./hive-mind.sh <story-file> <project-name>"
    echo "Example: ./hive-mind.sh user-stories/login.md user-management"
    exit 1
fi

echo "🧠 ACTIVATING ENTERPRISE HIVE MIND..."
echo "📄 Processing: $STORY_FILE"
echo "🏗️ Project: $PROJECT_NAME"

# Create project structure
mkdir -p "output/$PROJECT_NAME"
cd "output/$PROJECT_NAME"

echo "🔍 PHASE 1: ANALYST AGENT - Domain Analysis"
claude code --agent analyst \
    --context "../../.claude-agents/analyst.md" \
    --input "../../$STORY_FILE" \
    --output "analysis/" \
    --prompt "Perform comprehensive domain analysis following DDD principles"

echo "🛠️ PHASE 2: BUILDER AGENT - Implementation"
claude code --agent builder \
    --context "../../.claude-agents/builder.md" \
    --input "analysis/" \
    --output "implementation/" \
    --prompt "Implement enterprise solution using design patterns"

echo "✅ PHASE 3: REVIEWER AGENT - Quality Assurance"
claude code --agent reviewer \
    --context "../../.claude-agents/reviewer.md" \
    --input "implementation/" \
    --output "review/" \
    --prompt "Comprehensive quality and security review"

echo "🎯 PHASE 4: ORCHESTRATOR - Final Integration"
claude code --agent orchestrator \
    --context "../../.claude-agents/orchestrator.md" \
    --input "review/" \
    --output "final/" \
    --prompt "Integrate all components and create deployment package"

echo "✨ HIVE MIND PROCESSING COMPLETE!"
echo "📁 Output available in: output/$PROJECT_NAME/"