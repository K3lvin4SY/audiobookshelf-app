/**
 * Client-only "Show explicit content" setting. When it's off, explicit items are hidden in this
 * app as if the server user couldn't access explicit content. Nothing is changed on the server,
 * and downloaded explicit items stay on the device.
 */
class ExplicitFilter {
  constructor(store) {
    this.store = store
  }

  get hiding() {
    return this.store.getters['getShowExplicitContent'] === false
  }

  /** Library item, local library item, or a podcast episode's `podcast` object */
  isExplicit(item) {
    if (!item) return false
    if (item.media?.metadata?.explicit) return true
    // Episodes from recent-episodes carry their podcast instead of a library item
    return !!item.podcast?.metadata?.explicit
  }

  isHidden(item) {
    return this.hiding && this.isExplicit(item)
  }

  /** Library items or local library items */
  items(list) {
    if (!this.hiding || !Array.isArray(list)) return list
    return list.filter((item) => !this.isExplicit(item))
  }

  /** Series, collections and playlists: drop hidden books and the group itself if nothing is left */
  series(series) {
    if (!this.hiding || !series) return series
    if (!Array.isArray(series.books)) return series
    const books = this.items(series.books)
    if (!books.length) return null
    return { ...series, books }
  }

  collection(collection) {
    return this.series(collection)
  }

  playlist(playlist) {
    if (!this.hiding || !playlist || !Array.isArray(playlist.items)) return playlist
    const items = playlist.items.filter((i) => !this.isExplicit(i.libraryItem))
    if (!items.length) return null
    return { ...playlist, items }
  }

  /** Any bookshelf entity (book, series, collection, playlist, episode, author) */
  entity(entity, entityName) {
    if (!this.hiding || !entity) return entity
    if (entityName === 'series') return this.series(entity)
    if (entityName === 'collections') return this.collection(entity)
    if (entityName === 'playlists') return this.playlist(entity)
    if (entityName === 'authors') return entity
    return this.isExplicit(entity) ? null : entity
  }

  entities(list, entityName) {
    if (!this.hiding || !Array.isArray(list)) return list
    return list.map((e) => this.entity(e, entityName)).filter((e) => !!e)
  }
}

export default ({ store }, inject) => {
  inject('explicit', new ExplicitFilter(store))
}
