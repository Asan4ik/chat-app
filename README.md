# Chat App

Design patterns final project: a Java messaging app combining six design patterns.

## Team and patterns

| Member  | Patterns                | Packages                |
|---------|-------------------------|-------------------------|
| Asanali | Observer, Facade        | `observer`, `facade`    |
| Sanzhar | Decorator, Strategy     | `decorator`, `strategy` |
| Mansur  | Adapter, Factory Method | `adapter`, `factory`    |

## How it fits together

Factory creates messages, Decorators process them (Strategy provides the encryption algorithm), the Adapter sends them over XMPP or REST, Observer notifies receivers, and the Facade is the entry point used by the CLI.

`ChatFacade.send` runs the whole pipeline: validate, create (Factory), log and encrypt (Decorators + Strategy), send (Adapter), decrypt and publish to subscribers (Observer). Transport and encryption can be swapped at runtime. `sendAsync` queues sends on a single worker thread and keeps their order; all pipeline access is serialized, so the non-thread-safe transport clients are safe to share.

## Try it

`Main` starts an interactive CLI with three participants (Asanali, Sanzhar, Mansur). Type text to send it, or use commands:

```
/help  /users  /join <name>  /leave <name>  /as <name>
/type text|image|system   /encrypt none|caesar|base64   /transport rest|xmpp
/inbox [name]   /wire   /burst <n>   /quit
```

Example: `/encrypt caesar`, `/transport xmpp`, `hello`, then `/wire` shows the encrypted text on the wire while `/inbox Sanzhar` shows plaintext.

## Run (Docker only, nothing else to install)

Requires only Docker.

```
docker compose run --rm dev     # compile and run from source
docker compose run --rm test    # run unit tests
docker compose run --rm app     # build the final image and run the jar
```

The `dev` and `test` services mount the project folder, so edit code in any editor and re-run the command. Maven dependencies are cached in a Docker volume after the first run.

## Workflow

- `main` is protected; merge through pull requests only.
- Branch per pattern: `feature/observer`, `feature/facade`, `feature/decorator`, `feature/strategy`, `feature/adapter`, `feature/factory`.
- Suggested merge order: factory, strategy, decorator, adapter, observer, facade.
- UML diagrams go in `docs/uml/`, screenshots in `docs/screenshots/`, the report in `docs/report/`.
- Shared contracts are agreed upfront; tell the team before changing them.

## Shared contracts

- `model.Message`, `model.User`, `model.MessageType`
- `decorator.MessageProcessor`
- `adapter.ChatTransport`
- `observer.MessageListener`
- `strategy.EncryptionStrategy`
- `factory.MessageFactory`
