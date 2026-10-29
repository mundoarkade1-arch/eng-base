# Eng base - status

## Objetivo
App Android nativo de estudos de Engenharia Civil com tutor de IA, plano diário, caderno de cálculos e foco.

## Tecnologia
Kotlin 2.0.20, Jetpack Compose (Material 3), AGP 8.5.2, minSdk 26, targetSdk 34.
IA via servidor próprio (Cloudflare Worker) que chama a API da Anthropic.

## Feito (v0.1.0, ainda não compilado)
- Abas: Hoje, Matérias, Tutor, Caderno, Foco
- Plano diário por dificuldade, salvo no aparelho
- Caderno com calculadora passo a passo e 6 fórmulas de engenharia civil
- Tutor de IA (precisa do servidor configurado)
- Ícone: engrenagem com viga I

## Secrets (só nomes)
- Worker: ANTHROPIC_API_KEY, APP_TOKEN
- App: endereço do Worker e código de acesso (digitados no app)

## Problemas conhecidos
- APK debug muda de assinatura a cada build: para atualizar sem desinstalar, falta configurar chave de release.
- Nada foi compilado ainda; primeiro build pode apontar erros.

## Próximas tarefas
1. Primeiro build no GitHub Actions
2. Servidor da IA
3. Chave de release e assinatura
4. Banco de questões offline, revisão espaçada, alarme do foco
