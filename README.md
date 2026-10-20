# NyxChess

Xadrez local para 2 jogadores (Pass-and-Play) em Kotlin + Jetpack Compose + Material 3.

## Como abrir

1. Abra a pasta NyxChess no Android Studio (Ladybug ou mais recente, JDK 17).
2. Aguarde o Gradle Sync (o Android Studio gera o gradle-wrapper.jar sozinho).
3. Rode no emulador ou aparelho com Android 7.0 (API 24) ou superior.
4. Para testar o motor de regras: ./gradlew test (inclui perft e mate do tolo).

## Como debugar

1. Selecione a configuração `app` e o build variant `debug` (View > Tool Windows > Build Variants).
2. Clique no ícone de bug (Debug 'app') ou Shift+F9; clique na margem esquerda do código para pôr breakpoints.
3. Os testes do motor também rodam em debug: clique direito em ChessEngineTest > Debug.
4. `ui-tooling` está incluído no debug (Layout Inspector e @Preview).

## Estrutura

1. domain/model: Piece, Move, Position, GameStatus.
2. domain/engine: ChessEngine (regras puras, sem Android; SAN incluso).
3. presentation: GameViewModel (StateFlow de GameUiState + SharedFlow de GameEvent), GameState.
4. ui/theme: tema Material 3 (cores dinâmicas) e paletas do tabuleiro.
5. ui/components: ChessBoard (Canvas, animações, arrastar e soltar), PieceGlyph, Hud.
6. ui/screens: MenuScreen e GameScreen (promoção, fim de jogo, controles).
7. util: SoundPlayer.

## Fluxo de dados

A UI envia intenções ao ViewModel (clique, soltar peça, promoção, desfazer). O ViewModel valida
com o ChessEngine, atualiza o GameUiState e emite GameEvent para som e vibração.

## Personalização

1. Novos temas de tabuleiro: adicione um item em BoardTheme (BoardPalette.kt).
2. Sons reais: troque o SoundPlayer por SoundPool com arquivos .ogg em res/raw.
3. Novos controles de tempo: adicione itens em TimeControl (GameState.kt).
