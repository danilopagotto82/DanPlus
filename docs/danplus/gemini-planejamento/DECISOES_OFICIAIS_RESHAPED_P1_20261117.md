# DANPLUS P−1 · DECISÕES OFICIAIS — CATÁLOGO COMPLEMENTAR RESHAPED
Data da decisão: 17/11/2026. Categoria: PLANEJAMENTO DOCUMENTAL, não autorização de implementação.

## Proveniência e separação
- Solicitações expressas do proprietário: RS-01 ADAPTAR; RS-02 INVESTIGAR; RS-03 ADAPTAR.
- Fonte auditada: https://github.com/DavidVamaiotu/NuvioTV-Reshaped/releases/tag/1.2.0-beta.2-autosync.5
- Evidências/limites: `D:\APK\Organizacao DanPlus 2.0\P1_AUDITORIA_NUVIO_RESHAPED_AUTOSYNC_20261117.md`.
- **Não somar RS ao inventário P−1 de 146 IDs.** Registro principal: `DECISOES_OFICIAIS_P1_20261117.md`, 36 registradas, 110 pendentes; B2-06 permanece PENDENTE.

## Matriz RS (7 candidatos; 6 decididos; 1 pendente)
| ID | Melhoria Reshaped | Decisão oficial | Critério e guarda de integração |
|---|---|---|---|
| RS-01 | Sincronizar legendas escutando o áudio (VAD após AutoSync V2) | **ADAPTAR** | Somente futuro protótipo opt-in **desligado por padrão**, sem substituir AutoSync V2 nem alterar passthrough; medir Fire/Shield, Exo/MPV, PT-BR e memória |
| RS-02 | Reconhecer falas e escolher legenda mais adequada (ASR inglês) | **INVESTIGAR** | Modelo opcional ~74 MB, nunca baixar automaticamente; preservar seleção PT-BR versus PT-PT; validar SHA256/licença/custo e necessidade real; dependente da avaliação RS-01 |
| RS-03 | Ordenar fontes considerando velocidade aprendida passivamente | **ADAPTAR** | Integrar ao planejamento B1-27 ampliado/B2-07 sem limitar fontes tentáveis ou contrariar preferências REMUX, áudio e perfil; não criar medições pesadas de velocidade |
| RS-04 | Miniaturas geradas localmente durante seek | **MANTER** | Preservar geração local e Seekr homologados; defeito de enquadramento visual registrado separadamente como BUG-THUMB-01, sem confundir com portar engine |
| RS-05 | Foco na fonte atual, resultados compartilhados e menus de legenda mais leves | **ADAPTAR** | Adaptar foco da fonte em reprodução com patch mínimo futuro; estudar menu de legendas sem presumir bug ou autorizar código |
| RS-06 | Google Sync, Live TV, Pill/Netflix Home e fontes adicionais | **PENDENTE** | Parecer técnico preliminar: não portar diretamente; Dan TV+ e identidade premium são independentes; não é decisão do proprietário |
| RS-07 | Notas de episódios/títulos via Simkl/MDBList e seletor da fonte | **INVESTIGAR** | Investigar apenas diferença do seletor de origem das notas e integração com perfis, sem duplicar MDBList/Simkl existentes |

## Confirmação oficial complementar de 17/11/2026
- RS-04 **MANTER** a engine atual (não aprova manter defeito visual da moldura).
- RS-05 **ADAPTAR** o foco na fonte atual e estudar os menus de legendas, sem implementação.
- RS-07 **INVESTIGAR** a escolha da origem das avaliações.
- **BUG VISUAL SEPARADO:** miniatura mostra recorte inferior segundo screenshot enviado; registrar como RS-04-UI sem contar como novo item RS nem alterar classificação RS-04.

## Situação e gate
- Decisões oficiais neste catálogo: **6/7 (RS-01, RS-02, RS-03, RS-04, RS-05, RS-07)**; restante **RS-06 PENDENTE**.
- Consulta/arquitetura/planejamento: PASS de leitura. Testes focados, build, smoke, aparelhos, benchmark de qualidade da legenda: **NÃO TESTADOS / NÃO EXECUTADOS**.
- **PROIBIDO agora:** modificar APK, branch, player, OTA, GitHub, Supabase, perfis, instalar, usar ADB, publicar, merge/push, baixar modelo e clonar código do Reshaped na base homologada.
- Próximo passo: aprofundar RS-06 e aprofundar o novo defeito visual da janela de miniatura, sem duplicar RS-04, sem alterar este registro sem autorização.
