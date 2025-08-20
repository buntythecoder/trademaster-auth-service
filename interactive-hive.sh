echo "🧠 ENTERPRISE HIVE MIND - INTERACTIVE MODE"
echo "=========================================="

while true; do
    echo ""
    echo "Available commands:"
    echo "  @ANALYST <task>     - Delegate to analyst agent"
    echo "  @BUILDER <task>     - Delegate to builder agent" 
    echo "  @REVIEWER <task>    - Delegate to reviewer agent"
    echo "  @ALL <task>         - Multi-agent collaboration"
    echo "  story <file>        - Process user story file"
    echo "  status              - Show agent status"
    echo "  exit                - Exit hive mind"
    echo ""
    
    read -p "🎯 ORCHESTRATOR> " command
    
    case $command in
        @ANALYST*)
            task=${command#@ANALYST }
            echo "🔍 Delegating to ANALYST: $task"
            claude code --agent analyst --context .claude-agents/analyst.md --prompt "$task"
            ;;
        @BUILDER*)
            task=${command#@BUILDER }
            echo "🛠️ Delegating to BUILDER: $task"
            claude code --agent builder --context .claude-agents/builder.md --prompt "$task"
            ;;
        @REVIEWER*)
            task=${command#@REVIEWER }
            echo "✅ Delegating to REVIEWER: $task"
            claude code --agent reviewer --context .claude-agents/reviewer.md --prompt "$task"
            ;;
        @ALL*)
            task=${command#@ALL }
            echo "🧠 ALL AGENTS collaborating on: $task"
            echo "This will run multi-agent coordination..."
            ;;
        story*)
            file=${command#story }
            echo "📄 Processing user story: $file"
            ./hive-mind.sh "$file" "interactive-$(date +%s)"
            ;;
        status)
            echo "🎯 ORCHESTRATOR: Active"
            echo "🔍 ANALYST: Ready for domain analysis"
            echo "🛠️ BUILDER: Ready for implementation"
            echo "✅ REVIEWER: Ready for quality assurance"
            ;;
        exit)
            echo "🧠 Hive mind shutting down..."
            break
            ;;
        *)
            echo "❌ Unknown command. Type 'exit' to quit."
            ;;
    esac
done