package com.ziqiphyzhou.flashcard.shared

/*
Coroutine ownership rules:
- ViewModels launch stateful work in viewModelScope.
- Activities launch short, UI-bound suspend work in lifecycleScope.
- Repository methods are main-safe and move blocking Room calls to Dispatchers.IO.
- Direct, ownerless coroutine scopes are forbidden because they outlive screens and can update
  detached views. CancellationException must always propagate.
*/
