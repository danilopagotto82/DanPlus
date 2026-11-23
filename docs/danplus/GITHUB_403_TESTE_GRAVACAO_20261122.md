# DanPlus 2.1.0 — GitHub 403: teste de gravação

- Data: 2026-11-22 (America/Sao_Paulo)
- Branch isolada: `chore/wetrakr-backend-prep-20261122`
- Base: `main` no commit `3715ee0de8345d617f48c2da7271716ed501dc5c`
- Objetivo: validar que a conexão GitHub passou a permitir criação de branch e gravação de arquivo.
- Escopo: checkpoint técnico sem alteração de código, credenciais, APK, Supabase ou `main`.

## Próxima etapa — WeTrakr

- Supabase de staging tem o Client ID público registrado; conexão Android ainda não aplicada nem testada.
- O `Client Secret` nunca deve ser enviado a TVs, registrado em Git ou incluído em artefatos.
- Antes de implementar, conferir estado atual e instruções `GEMINI.md`, revisar pacote preparado e diferenciar a chave pública usada pelo aplicativo de qualquer segredo de cliente.
- Preservar a 2.0.2 homologada e compilar somente após conclusão de todos os megalotes da 2.1.0.
