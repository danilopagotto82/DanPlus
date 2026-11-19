# DECISÕES OFICIAIS — DANPLUS P−1
Data de registro: 17/11/2026
Origem: mensagem expressa do proprietário neste chat. Escopo: classificação/planejamento das novidades Nuvio e Arc, sem autorização para implantação.

## Escolhas do proprietário
| ID | Tema | Decisão humana | Estado |
|---|---|---|---|
| A2-08 | Áudio HD no MPV em PCM multicanal, não bitstream HD | **INVESTIGAR** | PENDENTE de prova técnica |
| A2-11 | Decodificação direta no MPV com fallback | **INVESTIGAR** | PENDENTE de prova técnica |
| B2-11 | Consentimento para addons de rastreamento | **ADAPTAR** | Não implementado nesta etapa |
| A2-35 | Otimização do Projectivy via acessibilidade | **NÃO RECOMENDAR** | Excluir integração; preservar no inventário |
| A2-36 | Atualização automática de pôsteres | **ADAPTAR** | Não implementado nesta etapa |
| A2-45 | Fallback de trailers IMDb em baixa resolução | **ADAPTAR** | Não implementado nesta etapa; YouTube/Supabase continua missão independente |
| A2-48 | Marcar séries longas como assistidas sem congelar o Trakt | **ADAPTAR** | Ainda não implementado; preservar lote existente e melhorar atualização visual |
| A2-49 | Mostrar cartão de erro claro e evitar falso pedido de relatório | **ADAPTAR** | Ainda não implementado; preservar diagnóstico e ajustar falha de outras fontes |
| A2-50 | Conservar notas digitadas no relatório e impedir protetor de tela | **ADAPTAR** | Ainda não implementado; manter aparência atual |
| A2-51 | Resposta Dolby Digital no teste de formatos | **ADAPTAR** | Ainda não implementado; preservar tela e traduzir |
| A1-23 | Melhorar proteção de memória na Fire TV | **ADAPTAR** | Ainda não implementado; aproveitar proteções existentes e validar 32 bits |
| A2-26 | Recuperar buffer após avançar/voltar | **ADAPTAR** | Ainda não implementado; investigar comportamento e adaptar somente diferença comprovada |
| A2-27 | Evitar falta de memória virtual em dispositivos 32 bits | **ADAPTAR** | Ainda não implementado; manter limites já existentes e testar na Fire |
| A2-25 | Evitar travadas no início do filme ao abrir o cache de disco | **ADAPTAR** | Preservar cache existente; preparar fora da tela e usar reprodução direta se demorar |
| A2-28 | Cancelar buscas desnecessárias do próximo episódio | **ADAPTAR** | Manter carregamento antecipado útil, interromper buscas não mais necessárias |
| A2-29 | Não desperdiçar cache do filme no teste de formatos | **ADAPTAR** | Manter testes e isentar clipes locais de gravação duplicada |
| B1-22 | Evitar consultas repetidas de temperatura | **MANTER** | Já protegido na base; não duplicar implementação; testes físicos não realizados |
| A1-22 | Reduzir travadas ao sincronizar históricos | **ADAPTAR** | Planejar junto de A2-38; preservar dados e configuração; não implementado |
| A2-38 | Economizar espaço nos históricos sem perder dados | **ADAPTAR** | Planejar junto de A1-22; evitar backups redundantes e verificar recuperação; não implementado |
| B1-19 | Atualizar corretamente resultados da Busca ao voltar ao campo | **INVESTIGAR** | Comparar comportamento no aparelho; preservar Busca e Scout/Ask |
| B1-20 | Voltar ao último campo usado, voz ou texto | **ADAPTAR** | Preservar Busca, Scout/Ask e teclado da Fire; não implementado |
| B1-21 | Evitar mudança de posição da grade da Biblioteca ao voltar de Detalhes | **INVESTIGAR** | Reproduzir na Fire/Shield antes de alterar; preservar navegação premium |
| A2-30 | Evitar saltos errados de destaque entre linhas da Home | **ADAPTAR** | Planejar controle de foco atrasado; manter o visual aprovado; não implementado |
| A2-31 | Manter linha correta com comandos rápidos no controle | **ADAPTAR** | Planejar proteção de movimentação e testes no controle remoto; não implementado |
| A2-32 | Carregar pôsteres sem prejudicar a navegação | **INVESTIGAR** | Já há pré-carregamento e limites na base; comparar desempenho real antes de alterar |
| B1-03 | Atualizar mais rápido o estado de assistido ao abrir ou navegar | **INVESTIGAR** | Conferir a atualização real sem marcar títulos incorretos; ainda não testado |
| B1-04 | Impedir que séries desapareçam de Continuar assistindo | **ADAPTAR** | Preservar episódio, arte e títulos enquanto atualiza, sem duplicar a lógica existente; não implementado |
| B1-11 | Retomar episódio certo após Android encerrar o aplicativo | **INVESTIGAR** | Testar recuperação de episódio e posição sem alterar progresso existente; ainda não comprovado |
| B1-05 | Proteger cenas pós-créditos | **MANTER** | O DanPlus já reconhece cenas pós-créditos; preservar regra, validar comportamento futuro em aparelho |
| B1-17 | Melhorar opções de capítulos e títulos | **ADAPTAR** | Aproveitar apenas seletores úteis, mantendo capítulos atuais e navegação aprovada; sem implementação |
| B1-26 | Preservar o nome da série ao avançar episódio | **MANTER** | O DanPlus já preserva o título; não duplicar correção, monitorar falhas reais |
| B2-02 | Não perder o minuto do filme ao trocar de fonte | **ADAPTAR** | Preservar posição ao trocar fonte do mesmo filme, sem levar posição para outro episódio; não implementado |
| B2-04 | Mostrar “Buscando fonte” no próximo episódio | **MANTER** | Estado e mensagem já presentes no DanPlus; não duplicar mudança; requer teste em TV |
| B1-16 | Renovar automaticamente links expirados | **INVESTIGAR** | Preservar recuperação já existente; verificar diferenças antes de alterar |
| B1-27 | Recuperação inteligente: tentar todas as fontes elegíveis do mesmo addon com espera adaptativa | **ADAPTAR** | Ampliação solicitada após recomendação anterior MANTER; eliminar teto fixo de 3 trocas somente com critérios de exaustão e segurança; usar latência/primeiro dado observados, limites por fonte e orçamento global; continuidade sem repetição e sem alterar perfis; apenas planejar |
| B2-07 | Tentar próxima fonte Debrid quando a escolhida não resolve, inclusive no episódio seguinte | **ADAPTAR** | Integrar à mesma estratégia de recuperação, preservar fontes em cache, filtros e regras de reprodução; não implementado |

## Cobertura e separação
- 146 entradas no inventário: 36 decisões oficiais acima, 110 **PENDENTES**.
- As classificações e recomendações técnicas da IA não substituem as decisões do proprietário.
- NÃO RECOMENDAR não apaga o ID A2-35 nem justificativa; apenas retira do plano de integração.
- Referências: este registro oficial; QUADRO_DECISOES_P1_DANPLUS_28_REGISTRADAS_20261117.md é fotografia anterior (sandbox, não atualizado neste arquivo).
- Projeto protegido: D:\APK\Worktrees\DanPlus_2_0_Unificado_ML01_20261115
- Estado no precheck: branch danplus/2.0-ml01-integracao; HEAD 0ce90a0740656c903b9780d2d423966b15119697; modificação pré-existente ?? .wrangler/ (não tocar).

## Gates de execução
- **PROIBIDO nesta missão:** alterar fonte do APK, Git, branch, produção, banco, credenciais, publicar release/OTA, instalar, compilar ou atualizar os dispositivos.
- ML-P0: investigar crashes Compose Place was called on a node which was placed already, foco DPAD, linhas Home desaparecidas; comparar Arc2 vs DanPlus sem tratar correlação como causa.
- ML-P1: rastrear PCM/bitstream nas cadeias reais, memória Fire 32-bit e decodificação direta MPV; inspeção não equivale a teste.
- ML-P2: consentimento com privacidade, atualização de pôsteres e trailer SD; tudo requer revisão e gates antes de codificar.
- Testes/build/smoke destas decisões: **NÃO TESTADO**.

Última ação: B1-16 INVESTIGAR, B1-27 ADAPTAR (ampliação posterior do proprietário) e B2-07 ADAPTAR; 33 escolhas anteriores preservadas. B2-06 permanece PENDENTE. Total 36 decididas, 110 pendentes. Autorização restrita a auditoria/planejamento, sem código, instalações ou publicações.
