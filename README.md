# Decifrador de Senha (Disciplina de Sistema Distribuídos)*

O objetivo desse trabalho é desenvolver um sistema distribuído simples capaz de explorar o poder de vários processadores para acelerar o cumprimento de uma tarefa que exige computação intensiva. Em sua implementação, você irá incorporar recursos necessários para criar um sistema robusto, que é capaz de lidar com pacotes perdidos ou duplicados e com as possíveis falhas de clientes e servidores. Você também irá aprender o valor de criar um conjunto de abstrações em camadas na ligação entre protocolos de rede de baixo nível e aplicações de alto nível.

Seu sistema irá implementar um decifrador de senhas distribuído.

## Passos

1. Ao chegar uma requisição, o *server* a divide em intervalos iguais (atualmente está definido em intervalos de 500 algarismos).
2. Em seguida os intervalos são embaralhados.
3. O *server* adiciona em uma lista de requisições a serem decifradas.
4. O *server* retira uma Requisição da fila e pega um intervalo.
5. O *server* reinsere a Requisição no final da fila.
6. O *server* pega o primeiro Worker disponível.
7. Atribui um intervalo para ser verificado pelo *worker*.
8. Quando um *worker* responde, ele é reinserido na lista de workers disponíveis.

## Como usar

1. Execute o *server*.
2. Execute quantos *worker*s achar necessário.
3. Execute o *requester*. A senha a ser quebrada deve estar em SHA1, possuir apenas números inteiros e se especifidado a quantidade de caractéres.

*Disciplina cursada em 2013.
