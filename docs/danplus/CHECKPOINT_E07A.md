# Checkpoint E07A — DanPlus 2.1.0

## Base e inventário

- Worktree `D:\APK\Worktrees\DanPlus_2_1_0_E02_20261125`, branch `feat/danplus-2.1.0-integracao-20261125`. HEAD inicial local e remoto `a29189ce5654f1ad9e6745f9fd3c1a868a5165d5`. Worktree limpa no início. Main local `e67459cfb1042fe2e68f97b0a017f1411512bffe` e remota `04e5b4e1780c43f6de4e5627453985821c07d48b`.
- Referências históricas do Gemini cruzadas com o plano oficial, a matriz 146 e o inventário de evidências E00. O anexo MUD R3 registra MUD-001 e REQ16-01 para a marca e o nome do app. A matriz vigente aprova B1-32 e A1-12 para localização. O inventário específico E07A mencionado na missão não foi localizado nos anexos, na worktree nem na busca do Drive. As ocorrências corrigidas foram identificadas e confirmadas no código, nos recursos e na arte renderizada.

## Ocorrências corrigidas

| Superfície | Evidência e correção |
| --- | --- |
| Identidade DanPlus | O manifesto já usa o rótulo comercial DanPlus. O recurso `app_name` agora usa o mesmo nome em todos os idiomas e variantes. Removidas as 36 sobreposições de idiomas que repetiam DAN+, a de PT-BR e a de debug com Nuvio Arc Debug. Atualizadas as descrições de acessibilidade, telas iniciais, relatórios, biblioteca e o título das páginas locais de configuração. |
| PT-BR e recursos recentes | Atualizados textos de reprodução, backup, diálogos, configurações, relatórios, Indicações, trilhas de ASS/SSA e ordenação do E05. A cópia de uso de fontes e o selo IMDb receberam os três recursos traduzíveis que faltavam. Alinhado `formatted=false` na escala de interface. Removida a definição concorrente “Para Você” do mesmo ID PT-BR de “Descubra-me”. Validados os argumentos das listas de Blu-ray, do calendário e dos recursos existentes do E05. |
| Estatísticas | Rótulos e seções do HUD localizados na apresentação. As chaves de agrupamento, valores medidos, unidades e limiares permanecem os mesmos. O painel de diagnóstico da interface usa rótulos e números do idioma da interface. Razões técnicas de medição e valores provenientes de terceiros continuam sendo exibidos conforme a origem. |
| Banners de launcher | A inspeção das cinco famílias confirmou a inscrição Nuvio Arc em 79 variantes. A nova arte usa a palavra-marca DanPlus já aprovada e os símbolos de cada paleta existente. Preservados nomes, opções, arranjos, fundos por família e transparência. Os 158 WebP alterados continuam nas dimensões 640 × 360 e 1280 × 720. O banner padrão nas duas densidades, os ícones, a abertura ativa e o Signature Glass foram preservados. Recursos de arte upstream sem um uso visual ativo identificado permanecem no repositório. |
| Créditos | A tela identifica DanPlus e reconhece explicitamente Nuvio Arc, Nuvio TV, Tapframe e ysosrs123. O link de código-fonte aponta para DanPlus. O link upstream da nota de fontes, a GNU GPL v3.0 e os avisos de provedores e bibliotecas seguem disponíveis. Preservados os nomes oficiais, incluindo Nuvio Sync, conta Nuvio, Trakt, Simkl, MDBList, WeTrakr, TMDB, Dolby Vision, DTS-HD e TrueHD. |
| Compatibilidade | Aliases e manifesto, applicationIds Shield e Fire, namespace, assinaturas, OTA, banco, credenciais, dependências e MPV 0.1.12 ficaram fora do diff. Preservados caminhos e formatos de backup, `nuvio-collections.json`, deep links, headers, formatos de telemetria de reprodução e identificador enviado na autorização Floppy. Nenhum idioma habilitado ou removido. As traduções fora do PT-BR receberam apenas a remoção do rótulo DanPlus redundante. |

## Verificações e pendências

- **PASS.** Quatro regressões estáticas executadas com `python -B -m unittest scripts.tests.test_e07a_resources -v`. Cobrem unicidade e paridade dos recursos padrão/PT-BR, argumentos e plurais, DanPlus e Descubra-me, atribuições e nomes de serviços, e as referências e geometria dos 160 banners do manifesto.
- **PASS.** `git diff --check`, `git diff --cached --check` e parser PSI em 15 arquivos Kotlin, sem erros de sintaxe. Comparados os argumentos e quantidades ao HEAD inicial, resolvidas as 77 referências Kotlin introduzidas e revisadas as sobreposições de nome removidas. Conferência visual dos banners antigos e de amostras novas em fundo claro e escuro. Os 12 commits protegidos até E05-R1 estão na ancestralidade. Escopo de 246 arquivos, incluindo 158 WebP, 71 XML, 15 Kotlin, a regressão estática e este checkpoint.
- **NÃO TESTADO.** Compilação Kotlin/AAPT, build, APK, instalação, smoke em Shield e Fire, atualização efetiva dos banners no launcher, layout dos rótulos longos no HUD e captura Signature Glass. A revisão de texto concentrou-se no padrão e em PT-BR. Outros catálogos podem exibir texto legado fora do nome unificado.
- **Restrições executadas.** Nenhum Gradle, build, APK, instalação, push, merge ou release. Main e produção preservadas. E08 não iniciado.

**Próxima ação.** Auditar pelo Commander o commit filho de `a29189ce5654f1ad9e6745f9fd3c1a868a5165d5`, incluindo a regressão estática de recursos. Depois da liberação do coordenador, levar a matriz de captura E10 a Shield e Fire, passando pelo launcher, pelas alternativas de banner e pelo HUD em PT-BR.
