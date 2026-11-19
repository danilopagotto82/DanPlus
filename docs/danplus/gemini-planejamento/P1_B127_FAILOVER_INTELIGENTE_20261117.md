# DANPLUS P−1 — B1-27 AMPLIADO: recuperação inteligente e espera adaptativa
Data: 17/11/2026. Missão de **auditoria e arquitetura, sem implementação**.

## Decisão do proprietário
- B1-16 = INVESTIGAR; B1-27 = **ADAPTAR (AMPLIADO)**; B2-07 = ADAPTAR.
- Novo pedido expresso para B1-27: abandonar a barreira fixa de 3 trocas, tentar **todas as fontes elegíveis do addon atual** até êxito ou esgotamento, com inteligência, continuidade/persistência e tempo de espera sensível à qualidade da internet.
- Arquivo canônico `D:\APK\Organizacao DanPlus 2.0\DECISOES_OFICIAIS_P1_20261117.md`: **36/146 registradas, 110 pendentes**. 33 anteriores preservadas. B2-06 permanece **PENDENTE**.
- Nenhuma classificação é autorização de codificar, instalar ou publicar.

## Evidências do código atual (PRECHECK PASS)
- Worktree protegida `D:\APK\Worktrees\DanPlus_2_0_Unificado_ML01_20261115`; branch `danplus/2.0-ml01-integracao`; HEAD `0ce90a0740656c903b9780d2d423966b15119697`. Único untracked antigo: `?? .wrangler/`, **não tocar**.
- `PlayerRuntimeControllerErrorRecovery.kt` define `MAX_DEAD_SOURCE_FAILOVERS = 3`, `FAILOVER_SOURCE_LIST_WAIT_MS = 20_000L`, polling 250 ms. São **3 trocas após a primeira fonte**, não necessariamente um total de 3 fontes.
- `advanceToNextLiveSource` só age se `sourceSelectedAutomatically == true` e **antes do primeiro quadro** (`!hasRenderedFirstFrame`). Não tratar como mecanismo de recuperação de toda falha a qualquer momento.
- `SourceFailoverSelection.selectNext`: percorre candidatos da lista, respeita pedido atual, temporada/episódio, perfis, evita chaves já falhas; parâmetro `addonName` só restringe por addon quando informado. Na falha comum a busca pode abranger outros addons. **Priorizar o mesmo addon** exige adaptação.
- `SourceFailoverSelection.isAutoFailoverPlayable` exclui fontes externas, Usenet, torrents não Debrid, fontes não cacheadas, URLs fora de HTTP(S); proteção existente obrigatória.
- `PlayerRuntimeControllerStreams.kt`: lista de fontes pode carregar gradualmente (`sourceStreamsFetchCompleted`); trabalhar até finalização comprovada, sem assumir que a lista está completa cedo.
- `AutoSpeedup.kt` e `AutoSpeedupPolicy.kt` já medem primeiro byte/transferência para parte do player. `DiagnosticNetworkMonitor.kt` observa rede. Ainda **não foi comprovado** que essas medições alimentam o timeout de failover.

## Proposta funcional — FUTURA, NÃO EXECUTADA
1. Construir fila **sequencial** de fontes elegíveis do mesmo addon para o **mesmo filme/episódio** e perfil. Evitar duplicatas por chave estável, preservar preferências como REMUX e áudio dublado; não mudar de addon sem regra explícita.
2. Em erro definitivo de URL, avançar rapidamente. Em resposta lenta, aguardar segundo a latência real e dados efetivamente recebidos. Quando a reprodução progride, não abandonar fonte funcional.
3. Substituir máximo 3 pelo critério **até esgotar fontes elegíveis daquele addon**, respeitando cancelamento imediato pelo usuário e um **orçamento global de segurança** para não entrar em busca eterna. Se o addon ainda carrega resultados, aguardar a conclusão dentro do orçamento. Se não houver conclusão, informar que a busca expirou, não afirmar que todas as fontes foram testadas.
4. Persistência **leve por perfil + título/episódio + addon + sessão**: registrar chaves/falhas/duração para não repetir tentativa na mesma sequência, inclusive após reentrada curta se pertinente. TTL curto e reset ao mudar fonte/rede/episódio; não manter blacklist permanente nem gravar URLs assinadas/tokens no histórico.
5. Interface clara: `Testando fonte X/Y neste addon...`, `Buscando mais fontes...`, `Fontes do addon esgotadas`, `Cancelar`. Se esgotadas, oferecer outro addon como escolha/configuração **posterior**, sem forçar troca silenciosa e sem perder o minuto do filme.
6. Proteger memória e rede da Fire: uma tentativa ativa, não baixar múltiplos filmes simultâneos, limites de logs/cache, cancelamento ao trocar episódio, fonte, tela ou sair.
7. Separar **falha antes de iniciar** do **travamento durante reprodução**. Este último exige captura/retomada de posição, verificação de buffer e gate de segurança; não extrapolar a correção atual para meio do filme sem teste.

## Tempo adaptativo por conexão
- Sim, é viável. A **velocidade nominal** Wi-Fi/cabo/4G serve apenas de pista; a taxa do primeiro salto da API Android não mede necessariamente a internet até o addon/CDN (docs Android `NetworkCapabilities`).
- Medir sem speedtest pesado: qualidade da rota, tempo até resposta do addon, tempo até primeiro byte do vídeo, tamanho e ritmo do buffer, histórico recente **do mesmo host/addon**, incidentes e alteração de rede.
- Estratégia: perfil inicial conservador, ajustar espera por **host + conexão + tipo de fonte** (HTTP simples, Debrid, REMUX, servidor doméstico), com **mínimo/máximo validado em testes**; jamais declarar números de segundos como homologados sem medição.
- Diferenciar prazo de **encontrar fontes** (hoje lista fixa 20s) e de **carregar cada stream**. Quando rede está lenta mas responde, prolongar moderadamente; erro explícito ou host impossível, avançar sem esperar o timeout inteiro.
- Não aumentar todos os timeouts globais do Media3 indiscriminadamente: impacto em player, memória, outros fluxos. Ajustar somente o caminho de recuperação que for comprovado.
- Android Media3 permite configurar tempos de conexão/leitura (`DefaultHttpDataSource.Factory`), mas isso não resolve sozinho o tempo de pesquisa do addon.

## Integração futura, aceites e riscos
- Agrupar B1-27 com **B2-07 ADAPTAR**, B2-02 ADAPTAR (posição ao trocar), A2-49 ADAPTAR (erro claro), A2-28 ADAPTAR (cancelar buscas); B1-16 fica INVESTIGAR. Sem duplicar lógica.
- Aceite: com 7 fontes elegíveis e 6 falhando, tentar a 7ª; com 0 elegíveis informar; não repetir uma URL morta; não tentar fontes proibidas; continuar posição; ao cancelar parar; tratar lista parcial e addon que não responde; modo manual respeitado; sem congelamento em Fire 32-bit; sem loops. **Testes ainda NÃO EXECUTADOS**.
- Segurança: nunca confundir falta de resposta com certeza de falha; respeitar disponibilidade real e fontes elegíveis; evitar retry infinito; cuidar de APIs pagas/limites do addon; logs sem tokens.
- No momento **não há autorização para patch, build, ADB, instalação, OTA, push, merge ou publicação**. Estado: arquitetura documentada; implementação NÃO TESTADA/NÃO EXECUTADA.
