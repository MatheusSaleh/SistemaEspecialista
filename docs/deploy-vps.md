# Publicar o AutoLab numa VPS Linux

O proxy HTTPS fala com o Spring Boot pelo socket `/var/lib/autolab/run/autolab.sock`. O [Compose de produção](../compose.yaml) usa `network_mode: none`, sem porta publicada ou rede Docker compartilhada. O contêiner não recebe credenciais de banco, acesso ao Docker, nem diretórios de outros projetos. O socket é compartilhado por um diretório dedicado.

## Preparar a imagem e as permissões

Para evitar que a compilação dispute recursos com os serviços da VPS, construa a imagem em outra máquina na arquitetura da VPS. Use um checkout da mesma revisão para os comandos de Compose e para a imagem.

```sh
docker build --pull -t autolab:local .
docker save -o autolab.tar autolab:local
```

Na VPS, confirme que o UID numérico `10001` está reservado para o AutoLab. Identifique o grupo dos *workers* do proxy. O exemplo assume **Nginx instalado no host**, com o grupo `www-data`. Preserve o grupo real do proxy na sua instalação.

```sh
export AUTOLAB_PROXY_GID="$(getent group www-data | cut -d: -f3)"
test -n "$AUTOLAB_PROXY_GID" && test "$AUTOLAB_PROXY_GID" -ne 0
```

Com o grupo confirmado, crie o diretório e inicie somente o serviço do AutoLab.

```sh
sudo install -d -o root -g root -m 0755 /var/lib/autolab
sudo install -d -o 10001 -g "$AUTOLAB_PROXY_GID" -m 0750 /var/lib/autolab/run
docker load -i autolab.tar
docker compose up -d --no-build --pull never
sudo -u www-data curl --fail --unix-socket /var/lib/autolab/run/autolab.sock http://localhost/api/regras
```

Use o usuário do proxy no último comando. O socket deve ter modo `0660` e o diretório `0750`. Se uma parada abrupta deixar um socket antigo, confirme que o serviço está parado e que nenhum processo está usando o socket antes de remover apenas o arquivo `autolab.sock`. Em hosts com mapeamento de usuários do Docker ou SELinux, verifique o acesso dos dois processos ao diretório dedicado com as regras do próprio host.

## Configurar o proxy HTTPS

Adapte o domínio e os certificados no [exemplo do Nginx](../deploy/nginx-autolab.conf). Adicione um *virtual host* exclusivo para o AutoLab e execute `sudo nginx -t` antes de recarregar. O exemplo limita o corpo a 16 KiB, aplica limites por IP e usa o socket como único destino. Se o proxy existente também estiver em contêiner, monte apenas o diretório do socket, em modo somente leitura, e dê ao processo do proxy a permissão de grupo necessária.

## Verificar o isolamento

```sh
cid="$(docker compose ps -q autolab)"
docker inspect --format '{{.HostConfig.NetworkMode}} {{json .HostConfig.PortBindings}}' "$cid"
docker inspect --format '{{.HostConfig.ReadonlyRootfs}} {{json .HostConfig.CapDrop}} {{json .HostConfig.SecurityOpt}}' "$cid"
docker compose logs --tail 30 autolab
```

A rede deve ser `none`, sem mapeamentos de porta. A raiz deve estar somente leitura, com todas as capacidades Linux removidas e `no-new-privileges` ativo. Os limites são 0,5 vCPU, 512 MiB de memória e 128 processos ou threads. Confira o domínio por HTTPS e teste os outros serviços do proxy após recarregá-lo.

O isolamento do AutoLab cobre sua rede, montagens e privilégios. O kernel, o Docker e o proxy ainda são compartilhados. Mantenha-os atualizados, verifique no firewall que as portas dos bancos não estão públicas e use uma VM separada caso a política dos dados exija uma barreira distinta da do contêiner.
