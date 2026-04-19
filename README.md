# Trabalho Prático 1 – Sistemas Distribuídos
## Jogo da Forca Multijogador em Java

### Autores
- Diogo Baltazar (52264)
- Davide Cravo (52868)

---

# Descrição

Este projeto consiste no desenvolvimento de uma versão distribuída do clássico **Jogo da Forca**, implementada em **Java**, seguindo uma arquitetura **Cliente-Servidor**.

O sistema permite a participação de **2 a 4 jogadores** em simultâneo, utilizando comunicação por **sockets TCP/IP**.

---

# Estrutura do Projeto

```text
Projeto/
├── palavras
├── README.md
├── Relatorio.pdf
└── projeto1/
    ├── Servidor.java
    ├── Cliente.java
    └── ClienteHandler.java
```

---

# Ficheiro "palavras"

O ficheiro **palavras** contém a lista de palavras possíveis utilizadas no jogo.

## Importante

O ficheiro `palavras` deve estar acessível na diretoria onde o servidor é executado para funcionar bem na powershell, no eclipse basta estar na raiz do programa.

# Requisitos

- Java JDK 8 ou superior
- Terminal / consola
- Windows ou Linux

---

# Execução na powershell, dentro da diretoria do servidor/cliente, com o ficheiro 'palavras' lá dentro

## Iniciar o Servidor

```bash
java .\Servidor.java
```

## Iniciar Clientes

Abrir novos terminais e executar:

```bash
java .\Cliente.java
```

Executar entre **2 e 4 clientes**.

---

# Funcionamento do Jogo

- Todos os jogadores participam na mesma palavra;
- A máscara da palavra é comum a todos;
- Letras descobertas ficam visíveis para todos;
- Erros afetam a forca global;
- O jogador que acertar a palavra completa vence;
- Se vários acertarem na mesma ronda, todos vencem.

O jogo combina cooperação e competição.

---

# Protocolo de Comunicação

## Servidor → Cliente

```text
WELCOME <id> <players_total>
START <mask> <attempts> <timeout>
ROUND <numero> <mask> <attempts> <used_letters>
END WIN <ids> <word>
END LOSE <word>
FULL
```

## Cliente → Servidor

```text
GUESS <texto>
```

---

# Observações Finais

- O jogo encerra automaticamente após vitória ou derrota
