# DanPlus 2.1.0 — pacote canônico de preparação

**Versão escolhida pelo proprietário em 19/11/2026:** próxima versão **2.1.0**, partindo da **2.0.2/1644** homologada. **Não há APK 2.1.0, build, teste ou release executado.** Não alterar versionCode nem editar manifests por mera escolha do nome.

## Base que deve ser preservada no PC
- Fonte oficial de desenvolvimento: `D:\APK\Worktrees\DanPlus_OFICIAL_FONTE_SEGURA_20261119`, repositório `https://github.com/danilopagotto82/DanPlus`.
- APKs de referência da 2.0.2, hashes e instruções: `D:\APK\DanPlus_2_0_2_HOMOLOGADO`. Duplicatas antigas descartáveis por autorização expressa do proprietário, após verificar cópia idêntica.
- Origem para futuras instalações OTA anteriores: `https://github.com/danilopagotto82/DANPLUS-Shield-Arc31-Private/releases/tag/2.0.2`; manter o repositório antigo ativo.
- Chave de assinatura Shield (não compartilhar/publicar): `C:\Users\danil\AppData\Local\DanPlus\Shield\Signing\danplus-shield-release.p12`. Confirmar alias/senha e **impressão digital antes do próximo build**; presença do arquivo não garante assinatura compatível.
- Chave Fire (preservar): `C:\Users\danil\.android\debug.keystore`. Impressão digital conferida contra APK Fire 2.0.2: **PASS** via apksigner/keytool.
- Configuração privada: `local.properties` na fonte oficial é ignorada pelo Git e foi copiada com SHA256 idêntico da fonte OTA; backup fora do Git em `C:\Users\danil\AppData\Local\DanPlus\Protected_2_1_0`. Nunca imprimir valores. Não contém as configurações de assinatura Shield nem a chave fallback YouTube.
- SDK/JDK/Gradle e arquivos AAR, bibliotecas C++ e libdovi da fonte oficial: manter. O `libmpv-android` legado é um gitlink não importado, mas a dependência utilizada no Gradle atual é por Maven (`io.github.abdallahmehiz:mpv-android-lib:0.1.12`); validar dependências com Gradle antes de presumir bloqueio.
- Preservar integralmente projetos **independentes** (MasterFinance, ÓRBITA, DAN TV, DaNEXUS e Central).

## Mudanças já registradas — execução planejada, não implementada

**Documento único vigente da versão 2.1.0.** A orientação antes distribuída entre `GEMINI.md`, `PROXIMA_VERSAO_PRONTIDAO_20261119.md` e o índice para Gemini foi reconciliada aqui em 23/11/2026. Os **17 anexos técnicos históricos**, preservados em `docs/danplus/gemini-planejamento/`, são fontes auxiliares de especificação, decisões e rastreabilidade, e **não** constituem planos alternativos. Em 19/11, os 17 anexos haviam sido confrontados por SHA256 com os originais locais; isso não prova execução de recursos. Prioridade de confiança: código/configuração/runtime/testes atuais → Git → este plano → anexos históricos → conversas.

1. **ML00 — precheck e reconciliação**: verificar baseline real 2.0.2 vs fonte sanitizada; build-signing, libmpv/Maven, RAM/Fire, crash Detalhes ainda existente ou não; comparar itens já implementados para evitar duplicidade. Apenas leitura/inventário.
2. **ML01 — estabilidade se bug comprovado**: investigar crash Compose Detalhes (relatado originalmente na 2.0.0). Só aplicar correção se for reproduzido ou se houver evidência atual.
3. **ML02 — UX/Focus Dock**: LOGO-PTBR-01 no Descobrir/Biblioteca (prioridade PT-BR, fallback idioma **original oficial** da obra, nunca inglês por padrão); BUG-BACKDROP-01 (Descobrir/Indicações sem precisar visitar Detalhes); BUG-THUMB-01 (miniatura cortada sem trocar gerador); Seekr/DPAD.
4. **ML03–06**: calendário/séries e Trakt/Simkl, progressos e históricos; recuperação inteligente de fontes do mesmo addon; player/cache/Fire, legendas/áudio/capítulos; revisão visual de pedidos MUD/REQ16 já parcialmente entregues.
5. **ML07**: RLS/indicações, relatórios, backup e perfis com teste sintético e gate separado para Supabase real. Não publicar segredos.
6. **ML08**: trailers YouTube prioritariamente PT-BR com fallback e cache Supabase, missão separada. A chave fixa histórica foi retirada da nova fonte; fallback alternativo local opcional **não testado**. Não copiar chaves para Git.
7. **ML09**: OTA 2.0.2 já homologada; para 2.1.0 preservar assinaturas, versionCode >1644, os dois nomes/formats por ABI e canais GitHub antigo e novo até validar clientes legados.
8. **ML10**: regressão e smoke reais na Shield e Fire antes do release 2.1.0.
9. **ML11 opcional**: comparação automatizada Nuvio Original + ARC sem IA recorrente, sem merges/releases automáticos.

**Decisões preservadas**: matriz 146 com 36 decisões e 110 pendentes; RS-06/B2-06 pendentes; A2-35 não recomendar; 14 REQ16 sem duplicação de MUD. **Planos ≠ patches.**

## UI-HEADER-01 — DanPlus Signature Glass (APROVADO EM 23/11/2026; DP-ML06)

**Decisão do proprietário:** substituir visualmente o cabeçalho da Home pelo layout flutuante aprovado em prévia nesta conversa, mantendo o restante da interface homologada. **Planejado e aprovado para a 2.1.0; NÃO CODIFICADO, NÃO TESTADO, SEM BUILD.** Não criar megalote independente: integrar ao acabamento de UI do **DP-ML06**, depois da reconciliação da DP-ML00. Não duplicar os componentes MUD-003 (avatares), MUD-004 (navegação) e MUD-005 (relógio).

### Composição homologada da barra superior
1. **Logo DanPlus isolado à extrema esquerda**, fora da barra principal, sobre fundo transparente, sem cápsula/caixa própria.
2. **Menu de navegação principal** em cápsula horizontal única, flutuante, cantos bem arredondados, vidro preto/fumê translúcido, contorno/reflexo sutil; preservar **os menus, rótulos, traduções, ordem e ações reais do DanPlus** verificados no código — a prévia com rótulos em inglês é apenas referência visual, não lista funcional para copiar. **Não** incluir Configurações nesse menu.
3. **Item selecionado/focado** identificado por cápsula interna levemente mais clara, sem brilho exagerado; transições discretas compatíveis com DPAD.
4. **Relógio separado da cápsula principal**, somente caracteres legíveis sobre fundo totalmente transparente, na parte superior direita, preservando fuso, preferência 12/24 horas, atualização e MUD-005 já existentes.
5. **À extrema direita, cápsula menor exclusiva** com engrenagem **Configurações** à esquerda e avatar do **perfil ativo** à direita. **Decisão final explícita:** esta cápsula deve ser **MUITO mais translúcida do que a barra principal, quase transparente**, com preenchimento mínimo e contorno quase imperceptível; engrenagem/avatar devem continuar nítidos e acessíveis pelo controle remoto. Não aplicar esse nível extremo de transparência à barra central.
6. Proteger a arte hero/backdrop, sinopse, cards, áreas de segurança e hierarquia visual já aprovadas. Não remodelar Home nem alterar lógica de dados sob pretexto desta mudança.

### Diretrizes técnicas e aceite futuro
- Localizar e **reutilizar** componentes reais de cabeçalho, navegação, relógio e perfil (Kotlin/Compose) após inventário; preferir patch mínimo em branch/worktree isolada, sem mexer na 2.0.2 homologada nem alterar identidade/signing/OTA.
- Adaptar margens, dimensões e legibilidade a **Shield Pro 2017 e Fire TV 4K Max 2ª**, com TV/overscan e tamanhos de texto; navegação **DPAD completa** (esquerda/direita, foco inicial, retorno, engrenagem, mudança de perfil), sem elemento inalcançável.
- Preferir opacidade/transparência simples; usar desfoque de fundo **somente se medido e sem regressão** de FPS/RAM no Fire. Não introduzir efeito custoso apenas para reproduzir conceito.
- Fazer capturas **da Home real** em Shield e Fire e comparar lado a lado com a referência visual aprovada de 23/11/2026; conferir logo fora da barra, menu central, relógio sem fundo e especialmente a **cápsula de perfil quase invisível**; validar contraste sobre backdrops claros/escuros, foco, acessibilidade e ausência de clipping.
- Entrar no **megalote único de UI** junto às demais melhorias já priorizadas; **não gerar APK/build isolado** para este cabeçalho. Sem teste físico e comparação visual, status **NÃO TESTADO**, jamais PASS.

## Consolidação operacional e técnica — 23/11/2026

**Fonte única de verdade para planejamento:** este \`PLANO_DANPLUS_2_1_0.md\`. O antigo \`GEMINI.md\` (instruções de Gemini CLI/Antigravity), o relatório \`PROXIMA_VERSAO_PRONTIDAO_20261119.md\` e o índice separado de planejamento foram absorvidos e podem ser excluídos sem eliminar as especificações técnicas anexas. O planejamento não autoriza codificação, migração, build, instalação ou publicação.

### Identidade, versão, proveniência e hierarquia

- **Produto:** DanPlus, Android TV Kotlin/Jetpack Compose, **Shield Pro 2017** e **Fire TV Stick 4K Max de 2ª geração**. Preservar DPAD, perfis, dados, áudio, players, catálogo, Seekr e experiências homologadas. A referência de UI mais recente é **UI-HEADER-01 — DanPlus Signature Glass**, aprovada visualmente em 23/11/2026; exclusivamente planejamento para ML06.
- **Fonte oficial atual:** \`danilopagotto82/DanPlus\`, branch \`main\`, worktree local historicamente registrada \`D:\APK\Worktrees\DanPlus_OFICIAL_FONTE_SEGURA_20261119\`. Revalidar HEAD e estado antes de qualquer intervenção. Não usar caminhos antigos de worktrees já excluídas.
- **Última versão homologada:** 2.0.2 / versionCode **1644** (Shield: \`com.danplus.shield\`; Fire: \`com.danexus.tv.fire.debug\`). APKs da release **2.0.2 no GitHub legado**, não no snapshot sanitizado da fonte nova. Fonte exata de referência histórica: branch \`danplus/mlc1-202-release-only-20261119\`, commit \`c92187031a204a4a01530f0b8662205c598b1e81\` no repositório legado. Para a 2.1.0, \`versionCode >1644\` é condição futura, **não** alteração já feita.
- **Linhagem para auditorias:** monitorar **Nuvio Original** (\`NuvioMedia/NuvioTV\`) e **Nuvio ARC** (\`ysosrs123/NuvioTV-Fork\`) como referências independentes; Reshaped/ReSeq é fonte adicional opcional, não baseline automática. As decisões de adoção exigem diff, evidência e aceite, nunca cópia integral por versão.
- **Documentação antiga:** cabeçalhos de 17–19/11 podem descrever branches, worktrees, release 2.0.0 ou 2.0.1 já superadas. **Não** reinterpretar texto histórico como estado atual. O usuário autorizou em 19/11 a remoção das antigas cópias **locais** do DanPlus; a fonte oficial e a pasta homologada 2.0.2 foram preservadas conforme relatório documental. Não estender essa autorização a outros projetos ou arquivos.
- **Ferramentas:** ChatGPT normal / GPT-6 / raciocínio Alto para arquitetura e coordenação; Gemini/Antigravity para leitura e auditoria não destrutivas; Codex Alto somente para implementação Kotlin/Compose de maior risco; Commander/Git/PowerShell/Gradle para inventário, teste e build mecânicos. Não usar agentes caros para acompanhar processos automáticos.

### Segurança e pontos de bloqueio concretos antes do primeiro patch

1. **Equivalência de fonte** — o repositório atual é um snapshot **sanitizado**, não reconstrução já homologada da 2.0.2. O fallback fixo de chave YouTube foi removido; a propriedade local opcional \`YOUTUBE_INNERTUBE_FALLBACK_KEY\` (em \`local.properties\`/arquivo local apropriado) ainda não foi validada por build e reprodução. **Não** republicar uma versão sem testar o fallback em cenários de erro.
2. **Dependências MPV** — o gitlink legado \`libmpv-android\` não foi importado porque não havia URL \`.gitmodules\` verificada; a dependência Gradle citada como ativa é \`io.github.abdallahmehiz:mpv-android-lib:0.1.12\`. Confirmar resoluções reais antes de declarar FAIL ou copiar arquivos antigos.
3. **Assinatura e OTA** — manter chaves e \`applicationId\` de Shield/Fire e compatibilidade **in-place**; certificado Fire havia sido comparado com 2.0.2 com PASS documental em 19/11, mas assinatura Shield e build da nova fonte requerem pré-checagem. Não expor nomes/valores de credenciais, PINs, keystore ou configuração privada. Manter o canal GitHub legado até provar migração OTA em ambos os dispositivos.
4. **Workflows e segredos** — **sete workflows históricos desativados** em \`docs/danplus/workflows-legado-desativados\`; não reativar cron, automerge, release ou CI herdada automaticamente. Havia **dois alertas de chave Google no legado** conforme relato técnico de 19/11; confirmar restrição/validade na origem antes de fechar risco. \`.gitignore\` recebeu regras de arquivos de assinatura em trabalho local histórico, mas revalidar Git atual e arquivos rastreados.
5. **Crash Detalhes** — houve relato da \`IllegalStateException\` Compose no app 2.0.0; ainda **não existe reprodução comprovada na 2.0.2** no material consolidado. ML01 é condicional à reprodução ou diagnóstico demonstrado, não correção presumida.
6. **Backend / dados** — MUD-011 integração/credenciais Seekr em **HOLD** por segurança e termos; backend/RLS/indicações/Supabase apenas com dados sintéticos e gate humano específico para produção. Não copiar tokens, \`.wrangler\`, senhas, \`local.properties\` ou arquivos de assinatura para Git, chat, relatórios ou automação.

### Sequência única de execução por dependências

| Megalote | Escopo único | Aceite / condição |
| --- | --- | --- |
| **DP-ML00** | **PRECHECK somente leitura**: \`AGENTS.md\`, HEAD/branch/status, comparação 2.0.2 vs fonte sanitizada, dependências, assinaturas, segredos, processos concorrentes e mapeamento dos componentes. | Relatório objetivo PASS/FAIL/NÃO TESTADO, sem editar APK ou produzir build. |
| **DP-ML01** | **Condicional**: investigar e corrigir crash Detalhes Compose apenas se reproduzido; isolar DPAD/scroll/foco e proteger Home. | Causa e regressão demonstradas, senão não abrir patch especulativo. |
| **DP-ML02** | **UX/foco/DPAD**: BUG-BACKDROP-01 Descobrir/Indicações, LOGO-PTBR-01/REQ16-05 no Focus Dock Descobrir/Biblioteca, BUG-THUMB-01 na miniatura Seekr sem substituir gerador, ajustes isolados de foco. | Backdrop sem abrir Detalhes como contorno; logos PT-BR → idioma original da obra → neutro; cache/foco sem regressão; comparação em Fire e Shield. |
| **DP-ML03** | Continuar assistindo, calendário e séries por episódio, históricos, Trakt/Simkl, perfis e persistência. | Progresso, scrobble, ordem e dados preservados na troca de perfil e restart. |
| **DP-ML04** | Recuperar fontes elegíveis do **mesmo addon** sem loop infinito, cancelamento, deduplicação e retomada; subgate distinto para RAM/32 bits Fire, cache/seek/REMUX. | Tentativas limitadas, sem travar nem corromper posição; regressão Fire/Shield. |
| **DP-ML05** | Capítulos, áudio, legendas, ASS, MPV/Exo; RS-01 VAD **opt-in desligado por padrão**, RS-02 ASR apenas investigação, sem download automático de modelo. | Sem regressão HDR/DV, DTS, Dolby/Atmos/TrueHD, controle remoto ou sincronismo. |
| **DP-ML06** | UI existente MUD/REQ16, Indicações, Focus Dock, Para Você, relatórios/curadoria e **UI-HEADER-01 Signature Glass** em **um único megalote visual**, preservando opções aprovadas. | Screenshots reais antes/depois na Shield/Fire, DPAD, clock/perfil, fluidez, foco e opacidade especial da cápsula de perfil. **Sem build isolado** para header. |
| **DP-ML07** | Backend/RLS/Supabase e sincronização apenas em LAB; MUD-011 HOLD. | Testes sintéticos, rollback e aprovação separada para qualquer produção. |
| **DP-ML08** | **Missão isolada** trailers PT-BR dublados/legendados e fallback YouTube + cache Supabase, inclusive exemplos Minority Report e A.I. | Evidência de seleção, chave segura, falha de rede/fallback, cache, e comparação foco Home versus tela Detalhes. |
| **DP-ML09** | Continuidade OTA/identidade: a ponte **2.0.2 já homologada** fica intacta; garantir upgrade futuro 2.1.0 em ambos os canais/ABIs. | Assinaturas, \`versionCode\`, hashes, compatibilidade in-place, URLs e rollback. |
| **DP-ML10** | Gate final único de testes, build, smoke físico e regressão em Shield/Fire. | **PASS real** por variante + screenshot/UI + vídeo/áudio/DPAD/OTA, antes da publicação separada. |
| **DP-ML11 (opcional)** | Monitor de upstream **Nuvio Original + ARC** via metadados GitHub Actions, ledger/diff/dedup, sem IA recorrente nem merge/release automático. | Projeto de automação **NÃO IMPLEMENTADO**; revisão humana e escopo separado. |

**Matriz de mudanças:** 146 IDs Original/ARC (36 decisões, 110 pendentes) e 7 Reshaped (6 decididos, RS-06 pendente), conforme registros de 17–19/11. **B2-06 pendente; A2-35 NÃO RECOMENDAR.** \`INVESTIGAR\` não autoriza patch; \`MANTER\` não deve ser reimplementado. As **14 entradas MUD R3** e os **14 REQ16** são camadas distintas, com possíveis sobreposições; evitar duplicações como B1-27/B2-07/RS-03, MUD-006/REQ16-06, MUD-008/REQ16-05, MUD-013/BUG-THUMB-01. Não confundir aparência aprovada com código existente nem tratar imagem gerada como screenshot do APK.

### Referências técnicas preservadas (17 anexos; históricos, NÃO outros planos)

Pasta: \`docs/danplus/gemini-planejamento/\`. Apesar do nome legado, contém **arquivos de especificação e rastreabilidade**, não um plano ativo próprio. Origem documental: \`D:\APK\Organizacao DanPlus 2.0\` (cópias comparadas por SHA256 em 19/11, conforme relatório). Não executar scripts de continuidade antigos sem revalidar paths e versões.

| Anexo | Finalidade |
| --- | --- |
| \`DP_CODEX_00_LEIA_PRIMEIRO_20261117.md\` | Histórico, instruções e requisitos legados |
| \`DP_CODEX_01_MEGALOTES_E_GATES_20261117.md\` | Critérios técnicos detalhados dos megalotes |
| \`DP_CODEX_02_PRECHECK_ACEITE_E_PROTECOES_20261117.md\` | Proteções, aceitação e QA |
| \`DP_CODEX_03_MATRIZ_146_E_RS_20261117.md\` | Lista completa dos 146 IDs e decisões Reshaped |
| \`DP_CODEX_04_MUD_R3_PEDIDOS_PENDENTES_20261117.md\` | MUD R3, 14 REQ16, LOGO-PTBR e BUG-BACKDROP |
| \`DP_CODEX_05_RISCOS_CONTINUIDADE_20261117.md\` | Riscos e antirretrabalho |
| \`DP_CODEX_06_SCRIPT_INICIO_20261117.md\` | Roteiro histórico, não comando atual |
| \`DP_CODEX_07_CHECKPOINT_CONTINUIDADE_20261117.md\` | Checkpoint histórico |
| \`DP_CODEX_08_ESTUDO_AUTOMACAO_UPSTREAM_20261117.md\` | Estudo de automação upstream |
| \`DP_CODEX_09_MONITOR_DUPLO_ORIGINAL_ARC_20261118.md\` | Monitor das duas linhagens |
| \`DP_CODEX_10_SCRIPT_ML11_MONITOR_DUAL_20261118.md\` | Alternativa opcional de implantação da ML11 |
| \`DP_AUTOMACAO_GITHUB_SEM_CODEX_ESTUDO_20261118.md\` | Abordagem de GitHub Actions sem IA recorrente |
| \`DECISOES_OFICIAIS_P1_20261117.md\` | Decisões Original/ARC |
| \`DECISOES_OFICIAIS_RESHAPED_P1_20261117.md\` | Decisões Reshaped |
| \`BUG_THUMB_01_CORTE_INFERIOR_MOLDURA_20261117.md\` | Defeito e aceite de miniatura |
| \`P1_RESHAPED_ETAPA2_PREVIEWS_FONTES_NOTAS_20261117.md\` | Estudo de previews/fontes/notas |
| \`P1_B127_FAILOVER_INTELIGENTE_20261117.md\` | Failover inteligente dentro do addon |

**Regra de entrada para qualquer IA:** ler **este plano** primeiro; para implementação, ler \`D:\APK\AGENTS.md\` e **apenas os anexos pertinentes** ao megalote, confrontando com Git/código atual. Evitar varreduras completas repetitivas dos 17 anexos. Cada entrega deve registrar **Feito / Resultado (PASS, FAIL ou NÃO TESTADO) / Próximo passo**.

## Segurança/qualidade e pontos de parada
- Próxima missão recomendada: **DP-ML00 somente leitura**, em GPT-6 Alto + Commander; Codex Alto só para patch Kotlin/Compose. Antes de começar, verificar `AGENTS.md` de D:\APK, branch, HEAD, working tree, processos concorrentes, dependências e evidências reais.
- Para codificar: branch/worktree NOVA criada da fonte oficial limpa. Preservar a main. Sem trocar applicationId, certificados, assinatura, URL OTA ou banco de produção.
- Assinatura Shield / certificado Fire, Gradle build e teste de fallback YouTube: ainda não homologados para **nova fonte sanitizada**.
- 2 alertas Google API Key históricos permanecem no repositório legado. O repositório oficial novo não tinha alertas abertos na última consulta; não importar histórico.
- Sete workflows herdados desativados; não reativar sem revisão.
- Critério de entrega: PRECHECK → PLANO → PATCH → TESTE FOCADO → BUILD → SMOKE FIRE/SHIELD → DIFF/SEGREDOS → gate de publicação. **Não dizer PASS para o que não foi testado.**

## Limpeza expressamente autorizada
O proprietário autorizou descartar **todas as cópias antigas locais do DanPlus**, inclusive branches com alterações não commitadas, por considerar suficiente a 2.0.2 publicada e o plano futuro. Preservar, porém, os itens mínimos de assinatura, fonte oficial, plano, APKs 2.0.2 e arquivos de outros projetos listados acima. Registrar os diretórios realmente excluídos e o espaço efetivamente recuperado, sem inventar sucesso.


## Encerramento da limpeza local — evidência de 19/11/2026

**Executado e confirmado:** o proprietário autorizou descartar definitivamente todas as cópias antigas locais do DanPlus, inclusive alterações dirty. As worktrees e raízes anteriores foram eliminadas; em `D:\APK` restam somente:
- `D:\APK\Worktrees\DanPlus_OFICIAL_FONTE_SEGURA_20261119` — fonte oficial para 2.1.0, Git limpo, esta documentação e Gemini;
- `D:\APK\DanPlus_2_0_2_HOMOLOGADO` — dois APKs homologados 2.0.2, hashes verificados e log de exclusão;
- `D:\APK\AGENTS.md` — instruções locais atualizadas.

O código exato da ponte 2.0.2 continua no **GitHub legado**, commit `c92187031a204a4a01530f0b8662205c598b1e81`; não depende de worktree antiga no PC. A documentação dos megalotes está na **main do GitHub oficial**.

**Protegido fora do Git:** `local.properties` privado na fonte oficial e backup em `C:\Users\danil\AppData\Local\DanPlus\Protected_2_1_0`; certificado Shield `C:\Users\danil\AppData\Local\DanPlus\Shield\Signing\danplus-shield-release.p12`; certificado Fire `C:\Users\danil\.android\debug.keystore`. O certificado Fire foi comparado ao APK 2.0.2 com resultado PASS. Shield: alias/senha ainda NÃO TESTADOS para build futuro.

**Espaço medido:** C: 55,88 GB → 56,77 GB livres; D: 130,09 GB → 193,43 GB livres (**+63,34 GB**); projetos independentes preservados. O relatório completo de limpeza está em `D:\APK\DanPlus_2_0_2_HOMOLOGADO\FECHAMENTO_LIMPEZA_DANPLUS_20261119.md`.

**Próxima etapa ainda não realizada:** DP-ML00, sem gerar APK 2.1.0; a numeração 2.1.0 é decisão de produto, não build concluído. Não executar comandos históricos que usem caminhos das worktrees excluídas.
