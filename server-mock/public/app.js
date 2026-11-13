/**
 * RadioAuto ES - Servidor Mock y Banco de Pruebas Web
 * Script de cliente en JavaScript estándar sin librerías externas ni dependencias
 */

// Estado global de la aplicación
const state = {
  stations: [],
  filteredStations: [],
  currentStation: null,
  isPlaying: false,
  activeView: 'all',          // 'all' | 'favorites'
  activeCountry: 'all',       // 'all' | nombre del país
  activeGenre: 'all',         // 'all' | nombre del género
  searchQuery: '',            // texto de búsqueda en minúsculas
  favorites: new Set(),       // Set de identificadores de emisoras favoritas
  audioElement: null
};

// Referencias a los elementos del DOM
const elements = {
  themeToggleBtn: document.getElementById('btn-toggle-theme'),
  themeIcon: document.getElementById('theme-icon'),
  searchInput: document.getElementById('search-input'),
  clearSearchBtn: document.getElementById('btn-clear-search'),
  
  // Filtros de vista y conteos
  filterAllBtn: document.getElementById('filter-all-btn'),
  filterFavoritesBtn: document.getElementById('filter-favorites-btn'),
  favCounterBadge: document.getElementById('fav-counter-badge'),

  // Filtros de país
  countryFilters: document.getElementById('country-filters'),
  countrySelect: document.getElementById('country-select'),

  // Filtros de género
  genreFilters: document.getElementById('genre-filters'),
  genreSelect: document.getElementById('genre-select'),

  stationsContainer: document.getElementById('stations-container'),
  stationsCounter: document.getElementById('stations-counter'),
  toast: document.getElementById('toast-notification'),
  
  // Reproductor persistente
  playerLogo: document.getElementById('player-logo'),
  playerTitle: document.getElementById('player-title'),
  playerSubtitle: document.getElementById('player-subtitle'),
  playerFavBtn: document.getElementById('player-fav-btn'),
  playerFavIcon: document.getElementById('player-fav-icon'),
  playerLiveBadge: document.getElementById('player-live-badge'),
  playerPlayBtn: document.getElementById('player-play-btn'),
  playerPrevBtn: document.getElementById('player-prev-btn'),
  playerNextBtn: document.getElementById('player-next-btn'),
  playIcon: document.getElementById('play-icon'),
  pauseIcon: document.getElementById('pause-icon'),
  playerStatusBadge: document.getElementById('player-status-badge'),
  soundEqualizer: document.getElementById('sound-equalizer'),
  volumeSlider: document.getElementById('volume-slider'),
  btnMute: document.getElementById('btn-mute'),
  audioStream: document.getElementById('audio-stream'),
  
  // Modal de edición / creación
  modal: document.getElementById('modal-station'),
  modalTitle: document.getElementById('modal-title'),
  form: document.getElementById('form-station'),
  formIsEdit: document.getElementById('form-is-edit'),
  inputStationId: document.getElementById('station-id'),
  inputStationName: document.getElementById('station-name'),
  inputStationGenre: document.getElementById('station-genre'),
  inputStationCountry: document.getElementById('station-country'),
  inputStationStream: document.getElementById('station-stream'),
  inputStationLogo: document.getElementById('station-logo'),
  inputStationDesc: document.getElementById('station-desc'),
  btnCloseModal: document.getElementById('btn-close-modal'),
  btnCancelModal: document.getElementById('btn-cancel-modal'),
  btnAddStation: document.getElementById('btn-add-station'),
  btnResetCatalog: document.getElementById('btn-reset-catalog')
};

/**
 * Inicializa la aplicación cargando tema, favoritos y catálogo de emisoras
 */
function initApp() {
  state.audioElement = elements.audioStream;
  loadFavoritesFromStorage();
  setupTheme();
  setupEventListeners();
  setupAudioEvents();
  loadStations();
}

/**
 * Carga los identificadores de emisoras favoritas almacenadas en localStorage
 */
function loadFavoritesFromStorage() {
  try {
    const raw = localStorage.getItem('radioauto_web_favorites');
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        state.favorites = new Set(parsed.map(String));
      }
    }
  } catch (err) {
    console.warn('No se pudieron leer las favoritas de localStorage:', err);
    state.favorites = new Set();
  }
}

/**
 * Guarda el conjunto actual de favoritas en localStorage
 */
function saveFavoritesToStorage() {
  try {
    const arr = Array.from(state.favorites);
    localStorage.setItem('radioauto_web_favorites', JSON.stringify(arr));
  } catch (err) {
    console.error('Error al guardar favoritas en localStorage:', err);
  }
}

/**
 * Alterna la condición de favorita para una emisora dada
 * @param {string} stationId - Identificador de la emisora
 */
function toggleFavorite(stationId) {
  const sid = String(stationId);
  const isNowFav = !state.favorites.has(sid);

  if (isNowFav) {
    state.favorites.add(sid);
  } else {
    state.favorites.delete(sid);
  }

  // Actualizar objeto en memoria
  const station = state.stations.find(s => String(s.id) === sid);
  if (station) {
    station.isFavorite = isNowFav;
    station.favorite = isNowFav;
    showToast(
      isNowFav ? `⭐ Añadida a favoritas: ${station.name}` : `Eliminada de favoritas: ${station.name}`,
      'info'
    );
  }

  saveFavoritesToStorage();
  updateFavoritesCounter();
  updatePlayerFavoriteIcon();

  // Si estamos en la vista de favoritas o necesitamos refrescar la tarjeta
  if (state.activeView === 'favorites') {
    filterStations();
  } else {
    updateCardFavoriteUI(sid, isNowFav);
  }
}

/**
 * Actualiza visualmente el botón de estrella de una tarjeta de emisora
 * @param {string} stationId - Identificador de la emisora
 * @param {boolean} isFavorite - true si está marcada como favorita
 */
function updateCardFavoriteUI(stationId, isFavorite) {
  const card = document.querySelector(`.station-card[data-id="${stationId}"]`);
  if (!card) return;

  const btnFav = card.querySelector('.btn-card-fav');
  if (btnFav) {
    btnFav.innerHTML = isFavorite ? '⭐' : '☆';
    btnFav.title = isFavorite ? 'Quitar de favoritas' : 'Añadir a favoritas';
    if (isFavorite) {
      btnFav.classList.add('is-favorite');
    } else {
      btnFav.classList.remove('is-favorite');
    }
  }
}

/**
 * Actualiza el contador de favoritas en la interfaz de usuario
 */
function updateFavoritesCounter() {
  if (elements.favCounterBadge) {
    elements.favCounterBadge.textContent = `(${state.favorites.size})`;
  }
}

/**
 * Actualiza el icono de favorita en el reproductor persistente inferior
 */
function updatePlayerFavoriteIcon() {
  if (!state.currentStation) {
    elements.playerFavBtn.style.display = 'none';
    return;
  }

  elements.playerFavBtn.style.display = 'inline-flex';
  const isFav = state.favorites.has(String(state.currentStation.id));
  elements.playerFavIcon.textContent = isFav ? '⭐' : '☆';
  elements.playerFavBtn.title = isFav ? 'Quitar de favoritas' : 'Añadir a favoritas';
  if (isFav) {
    elements.playerFavBtn.classList.add('is-favorite');
  } else {
    elements.playerFavBtn.classList.remove('is-favorite');
  }
}

/**
 * Configura el tema visual claro u oscuro desde localStorage o preferencias del sistema
 */
function setupTheme() {
  const savedTheme = localStorage.getItem('radioauto_theme');
  const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
  const currentTheme = savedTheme ? savedTheme : (prefersDark ? 'dark' : 'light');
  document.documentElement.setAttribute('data-theme', currentTheme);
  updateThemeIcon(currentTheme);
}

/**
 * Alterna entre el tema visual claro y oscuro guardándolo en localStorage
 */
function toggleTheme() {
  const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
  const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', newTheme);
  localStorage.setItem('radioauto_theme', newTheme);
  updateThemeIcon(newTheme);
}

/**
 * Actualiza el icono del botón de cambio de tema según el modo activo
 * @param {string} theme - 'dark' o 'light'
 */
function updateThemeIcon(theme) {
  elements.themeIcon.innerHTML = theme === 'dark' ? '&#9788;' : '&#9790;';
}

/**
 * Configura los eventos de interacción de la interfaz de usuario
 */
function setupEventListeners() {
  // Cambio de tema
  elements.themeToggleBtn.addEventListener('click', toggleTheme);

  // Búsqueda en tiempo real
  elements.searchInput.addEventListener('input', (event) => {
    state.searchQuery = event.target.value.trim().toLowerCase();
    elements.clearSearchBtn.style.display = state.searchQuery.length > 0 ? 'block' : 'none';
    filterStations();
  });

  elements.clearSearchBtn.addEventListener('click', () => {
    elements.searchInput.value = '';
    state.searchQuery = '';
    elements.clearSearchBtn.style.display = 'none';
    filterStations();
  });

  // Filtros de vista rápida (Todas / Favoritas)
  elements.filterAllBtn.addEventListener('click', () => {
    state.activeView = 'all';
    elements.filterAllBtn.classList.add('active');
    elements.filterFavoritesBtn.classList.remove('active');
    filterStations();
  });

  elements.filterFavoritesBtn.addEventListener('click', () => {
    state.activeView = 'favorites';
    elements.filterFavoritesBtn.classList.add('active');
    elements.filterAllBtn.classList.remove('active');
    filterStations();
  });

  // Filtros de país (píldoras rápidas)
  elements.countryFilters.addEventListener('click', (event) => {
    const pill = event.target.closest('.pill');
    if (!pill) return;
    
    elements.countryFilters.querySelectorAll('.pill').forEach(btn => btn.classList.remove('active'));
    pill.classList.add('active');
    
    const country = pill.getAttribute('data-country');
    state.activeCountry = country;
    elements.countrySelect.value = country === 'all' ? 'all' : country;
    filterStations();
  });

  // Selector desplegable de países
  elements.countrySelect.addEventListener('change', (event) => {
    const selected = event.target.value;
    state.activeCountry = selected;

    elements.countryFilters.querySelectorAll('.pill').forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-country') === selected);
    });

    filterStations();
  });

  // Filtros de género (píldoras rápidas)
  elements.genreFilters.addEventListener('click', (event) => {
    const pill = event.target.closest('.pill');
    if (!pill) return;
    
    elements.genreFilters.querySelectorAll('.pill').forEach(btn => btn.classList.remove('active'));
    pill.classList.add('active');
    
    const genre = pill.getAttribute('data-genre');
    state.activeGenre = genre;
    elements.genreSelect.value = genre === 'all' ? 'all' : genre;
    filterStations();
  });

  // Selector desplegable de géneros
  elements.genreSelect.addEventListener('change', (event) => {
    const selected = event.target.value;
    state.activeGenre = selected;

    elements.genreFilters.querySelectorAll('.pill').forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-genre') === selected);
    });

    filterStations();
  });

  // Botón de favorita en el reproductor inferior
  elements.playerFavBtn.addEventListener('click', () => {
    if (state.currentStation) {
      toggleFavorite(state.currentStation.id);
    }
  });

  // Controles de reproducción
  elements.playerPlayBtn.addEventListener('click', () => {
    if (!state.currentStation) {
      if (state.filteredStations.length > 0) {
        playStation(state.filteredStations[0]);
      }
      return;
    }
    
    if (state.isPlaying) {
      pauseStation();
    } else {
      resumeStation();
    }
  });

  elements.playerNextBtn.addEventListener('click', playNextStation);
  elements.playerPrevBtn.addEventListener('click', playPreviousStation);

  // Control de volumen y silencio
  elements.volumeSlider.addEventListener('input', (event) => {
    const vol = parseFloat(event.target.value);
    state.audioElement.volume = vol;
    state.audioElement.muted = false;
  });

  elements.btnMute.addEventListener('click', () => {
    state.audioElement.muted = !state.audioElement.muted;
    showToast(state.audioElement.muted ? 'Audio silenciado' : 'Audio activado', 'info');
  });

  // Modal para añadir/editar emisoras
  elements.btnAddStation.addEventListener('click', openAddModal);
  elements.btnCloseModal.addEventListener('click', closeModal);
  elements.btnCancelModal.addEventListener('click', closeModal);
  elements.form.addEventListener('submit', handleFormSubmit);

  // Restaurar catálogo
  elements.btnResetCatalog.addEventListener('click', resetCatalog);
}

/**
 * Configura los eventos del elemento HTML5 de audio nativo
 */
function setupAudioEvents() {
  const audio = state.audioElement;

  audio.addEventListener('loadstart', () => {
    updatePlayerStatus('Conectando...', 'normal');
  });

  audio.addEventListener('waiting', () => {
    updatePlayerStatus('Cargando buffer...', 'normal');
  });

  audio.addEventListener('playing', () => {
    state.isPlaying = true;
    updatePlayerControlsUI(true);
    updatePlayerStatus('En emisión', 'live');
    showToast(`Reproduciendo: ${state.currentStation.name}`, 'success');
  });

  audio.addEventListener('pause', () => {
    state.isPlaying = false;
    updatePlayerControlsUI(false);
    updatePlayerStatus('En pausa', 'normal');
  });

  audio.addEventListener('error', (event) => {
    state.isPlaying = false;
    updatePlayerControlsUI(false);
    updatePlayerStatus('Error de emisión', 'error');
    console.error('Error de audio HTML5:', event);
    showToast(`Error al reproducir ${state.currentStation?.name || 'la emisora'}. Comprueba URL o CORS.`, 'error');
  });
}

/**
 * Carga el catálogo maestro de emisoras desde el endpoint REST /api/stations
 */
async function loadStations() {
  try {
    elements.stationsCounter.textContent = 'Cargando catálogo maestro...';
    const response = await fetch('/api/stations');
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    const data = await response.json();

    // Sincronizar estado de favoritas combinando JSON y localStorage
    state.stations = data.map(st => {
      const sid = String(st.id);
      if (st.isFavorite || st.favorite) {
        state.favorites.add(sid);
      }
      const isFav = state.favorites.has(sid);
      return {
        ...st,
        id: sid,
        isFavorite: isFav,
        favorite: isFav
      };
    });

    saveFavoritesToStorage();
    updateFavoritesCounter();
    populateCountryFilters(state.stations);
    populateGenreFilters(state.stations);
    filterStations();
  } catch (error) {
    console.error('Error al cargar emisoras:', error);
    elements.stationsCounter.textContent = 'Error al cargar emisoras desde /api/stations';
    showToast('No se pudo conectar con el servidor de emisoras', 'error');
  }
}

/**
 * Genera el selector de países con el conteo de emisoras disponibles por país
 * @param {Array} stations - Lista completa de emisoras
 */
function populateCountryFilters(stations) {
  const countryCounts = {};
  stations.forEach(st => {
    const c = (st.country || 'Internacional').trim();
    countryCounts[c] = (countryCounts[c] || 0) + 1;
  });

  const sortedCountries = Object.keys(countryCounts).sort((a, b) => a.localeCompare(b, 'es'));
  
  elements.countrySelect.innerHTML = '<option value="all">Todos los países (' + stations.length + ')</option>';
  sortedCountries.forEach(country => {
    const opt = document.createElement('option');
    opt.value = country;
    opt.textContent = `${country} (${countryCounts[country]})`;
    elements.countrySelect.appendChild(opt);
  });
}

/**
 * Extrae todos los géneros individuales y genera el selector desplegable completo
 * @param {Array} stations - Lista completa de emisoras
 */
function populateGenreFilters(stations) {
  const genreCounts = {};

  stations.forEach(st => {
    if (!st.genre) return;
    // Dividir géneros compuestos por comas o barras
    const parts = st.genre.split(/[,/]/).map(g => g.trim()).filter(Boolean);
    parts.forEach(g => {
      genreCounts[g] = (genreCounts[g] || 0) + 1;
    });
  });

  const sortedGenres = Object.keys(genreCounts).sort((a, b) => a.localeCompare(b, 'es'));
  
  elements.genreSelect.innerHTML = '<option value="all">Todos los géneros</option>';
  sortedGenres.forEach(genre => {
    const opt = document.createElement('option');
    opt.value = genre;
    opt.textContent = `${genre} (${genreCounts[genre]})`;
    elements.genreSelect.appendChild(opt);
  });
}

/**
 * Aplica los filtros de vista, país, género y búsqueda sobre el catálogo completo
 */
function filterStations() {
  state.filteredStations = state.stations.filter(station => {
    const sid = String(station.id);
    const isFav = state.favorites.has(sid);

    // Filtro por vista (Todas o Favoritas)
    if (state.activeView === 'favorites' && !isFav) {
      return false;
    }

    // Filtro por país
    if (state.activeCountry !== 'all' && station.country !== state.activeCountry) {
      return false;
    }

    // Filtro por género (búsqueda por inclusión para admitir etiquetas compuestas)
    if (state.activeGenre !== 'all') {
      const gLower = (station.genre || '').toLowerCase();
      const targetLower = state.activeGenre.toLowerCase();
      if (!gLower.includes(targetLower)) {
        return false;
      }
    }

    // Filtro por término de búsqueda en texto
    if (state.searchQuery) {
      const q = state.searchQuery;
      const matchName = station.name && station.name.toLowerCase().includes(q);
      const matchGenre = station.genre && station.genre.toLowerCase().includes(q);
      const matchCountry = station.country && station.country.toLowerCase().includes(q);
      const matchBroadcaster = station.broadcaster && station.broadcaster.toLowerCase().includes(q);
      const matchDesc = station.description && station.description.toLowerCase().includes(q);

      if (!matchName && !matchGenre && !matchCountry && !matchBroadcaster && !matchDesc) {
        return false;
      }
    }

    return true;
  });

  elements.stationsCounter.textContent = `Mostrando ${state.filteredStations.length} de ${state.stations.length} emisoras`;
  renderStations(state.filteredStations);
}

/**
 * Renderiza la lista de emisoras filtradas en tarjetas dentro de la cuadrícula
 * @param {Array} stations - Lista de emisoras a mostrar
 */
function renderStations(stations) {
  elements.stationsContainer.innerHTML = '';

  if (stations.length === 0) {
    const isFavView = state.activeView === 'favorites';
    elements.stationsContainer.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; padding: 48px 16px; color: var(--text-secondary);">
        <p style="font-size: 1.2rem; font-weight: 700;">
          ${isFavView ? '⭐ Aún no tienes emisoras favoritas guardadas' : 'No se encontraron emisoras con los filtros seleccionados'}
        </p>
        <p style="font-size: 0.9rem; margin-top: 8px;">
          ${isFavView ? 'Haz clic en la estrella (☆) de cualquier tarjeta para añadirla a tus favoritas y verla primero en Android Auto.' : 'Prueba a cambiar el término de búsqueda o restablecer los filtros de país y género.'}
        </p>
      </div>
    `;
    return;
  }

  stations.forEach(station => {
    const sid = String(station.id);
    const isCurrent = state.currentStation && String(state.currentStation.id) === sid;
    const isCurrentPlaying = isCurrent && state.isPlaying;
    const isFav = state.favorites.has(sid);

    const card = document.createElement('article');
    card.className = `station-card ${isCurrentPlaying ? 'is-playing' : ''}`;
    card.setAttribute('data-id', sid);

    // Resolver ruta del logotipo
    const rawLogo = station.logoUrl || station.logo_url || '';
    const logoSrc = rawLogo.startsWith('/') || rawLogo.startsWith('http') 
      ? rawLogo 
      : `/logos/${rawLogo || 'radio_default.svg'}`;

    const formatBadge = station.format ? `<span class="tag-format">${station.format}</span>` : '';

    card.innerHTML = `
      <div class="card-top">
        <img class="station-logo-img" src="${logoSrc}" alt="${station.name}" onerror="this.src='/logos/radio_default.svg'">
        <div class="station-info">
          <h3 class="station-name" title="${station.name}">${station.name}</h3>
          <div class="station-tags">
            <span class="tag-genre">${station.genre || 'Variedad'}</span>
            <span class="tag-country">${station.country || 'España'}</span>
            ${formatBadge}
          </div>
        </div>
        <button class="btn-card-fav ${isFav ? 'is-favorite' : ''}" onclick="toggleFavorite('${sid}')" title="${isFav ? 'Quitar de favoritas' : 'Añadir a favoritas'}">
          ${isFav ? '⭐' : '☆'}
        </button>
      </div>

      <p class="station-desc">${station.description || 'Emisión en directo de radio por streaming en alta calidad.'}</p>

      <div class="card-actions">
        <button class="btn-card-play" onclick="handleCardPlayClick('${sid}')">
          ${isCurrentPlaying ? 'Pausar' : 'Sintonizar'}
        </button>
        <button class="btn-card-test" onclick="testStationStream('${sid}')" title="Comprobar si el stream responde">
          Probar
        </button>
        <button class="btn-card-icon" onclick="openEditModal('${sid}')" title="Editar emisora">
          &#9998;
        </button>
        <button class="btn-card-icon" onclick="deleteStation('${sid}')" title="Eliminar emisora">
          &#128465;
        </button>
      </div>
    `;

    elements.stationsContainer.appendChild(card);
  });
}

/**
 * Controla la reproducción o pausa desde el botón de la tarjeta de una emisora
 * @param {string} stationId - Identificador único de la emisora
 */
function handleCardPlayClick(stationId) {
  const sid = String(stationId);
  const station = state.stations.find(s => String(s.id) === sid);
  if (!station) return;

  if (state.currentStation && String(state.currentStation.id) === sid) {
    if (state.isPlaying) {
      pauseStation();
    } else {
      resumeStation();
    }
  } else {
    playStation(station);
  }
}

/**
 * Carga e inicia la reproducción de una emisora en el reproductor de audio
 * @param {Object} station - Objeto con los datos de la emisora seleccionada
 */
function playStation(station) {
  state.currentStation = station;
  
  elements.playerTitle.textContent = station.name;
  elements.playerSubtitle.textContent = `${station.genre || 'Variedad'} • ${station.country || 'España'}`;
  
  const rawLogo = station.logoUrl || station.logo_url || '';
  const logoSrc = rawLogo.startsWith('/') || rawLogo.startsWith('http') 
    ? rawLogo 
    : `/logos/${rawLogo || 'radio_default.svg'}`;
  elements.playerLogo.src = logoSrc;
  elements.playerLogo.onerror = () => { elements.playerLogo.src = '/logos/radio_default.svg'; };
  
  updatePlayerFavoriteIcon();

  const stream = station.streamUrl || station.stream_url;
  state.audioElement.src = stream;
  state.audioElement.load();
  
  const playPromise = state.audioElement.play();
  if (playPromise !== undefined) {
    playPromise.catch(error => {
      console.warn('Reproducción diferida por política de autoplay:', error);
      updatePlayerControlsUI(false);
      updatePlayerStatus('Pausado', 'normal');
    });
  }
  
  updateCardsPlayingState();
}

/**
 * Pausa la reproducción del flujo de audio actual
 */
function pauseStation() {
  state.audioElement.pause();
  state.isPlaying = false;
  updatePlayerControlsUI(false);
  updateCardsPlayingState();
}

/**
 * Reanuda la reproducción del flujo de audio actual
 */
function resumeStation() {
  const playPromise = state.audioElement.play();
  if (playPromise !== undefined) {
    playPromise.catch(error => {
      console.error('Error al reanudar:', error);
      showToast('Error al reanudar stream', 'error');
    });
  }
}

/**
 * Avanza a la siguiente emisora de la lista filtrada actualmente
 */
function playNextStation() {
  if (state.filteredStations.length === 0) return;
  
  let nextIndex = 0;
  if (state.currentStation) {
    const currentIndex = state.filteredStations.findIndex(s => String(s.id) === String(state.currentStation.id));
    if (currentIndex !== -1 && currentIndex < state.filteredStations.length - 1) {
      nextIndex = currentIndex + 1;
    }
  }
  
  playStation(state.filteredStations[nextIndex]);
}

/**
 * Retrocede a la emisora anterior de la lista filtrada actualmente
 */
function playPreviousStation() {
  if (state.filteredStations.length === 0) return;
  
  let prevIndex = state.filteredStations.length - 1;
  if (state.currentStation) {
    const currentIndex = state.filteredStations.findIndex(s => String(s.id) === String(state.currentStation.id));
    if (currentIndex > 0) {
      prevIndex = currentIndex - 1;
    }
  }
  
  playStation(state.filteredStations[prevIndex]);
}

/**
 * Actualiza los iconos y estados visuales de los controles del reproductor inferior
 * @param {boolean} isPlaying - true si el audio se está reproduciendo activamente
 */
function updatePlayerControlsUI(isPlaying) {
  if (isPlaying) {
    elements.playIcon.style.display = 'none';
    elements.pauseIcon.style.display = 'block';
    elements.playerLiveBadge.style.display = 'inline-flex';
    elements.soundEqualizer.style.display = 'flex';
  } else {
    elements.playIcon.style.display = 'block';
    elements.pauseIcon.style.display = 'none';
    elements.playerLiveBadge.style.display = 'none';
    elements.soundEqualizer.style.display = 'none';
  }
}

/**
 * Actualiza la etiqueta de estado de conexión del reproductor
 * @param {string} text - Texto a mostrar (ej. 'Listo', 'En emisión')
 * @param {string} type - Tipo de estado: 'normal', 'live', 'error'
 */
function updatePlayerStatus(text, type = 'normal') {
  elements.playerStatusBadge.textContent = text;
  elements.playerStatusBadge.className = 'player-status';
  if (type === 'live') {
    elements.playerStatusBadge.classList.add('status-live');
  } else if (type === 'error') {
    elements.playerStatusBadge.classList.add('status-error');
  }
}

/**
 * Resalta la tarjeta de la emisora activa en la rejilla
 */
function updateCardsPlayingState() {
  const currentId = state.currentStation ? String(state.currentStation.id) : null;
  
  document.querySelectorAll('.station-card').forEach(card => {
    const cardId = card.getAttribute('data-id');
    const playBtn = card.querySelector('.btn-card-play');
    
    if (cardId === currentId && state.isPlaying) {
      card.classList.add('is-playing');
      if (playBtn) playBtn.textContent = 'Pausar';
    } else {
      card.classList.remove('is-playing');
      if (playBtn) playBtn.textContent = 'Sintonizar';
    }
  });
}

/**
 * Comprueba de forma independiente la disponibilidad de una emisora mediante una petición HEAD
 * @param {string} stationId - Identificador único de la emisora
 */
async function testStationStream(stationId) {
  const station = state.stations.find(s => String(s.id) === String(stationId));
  if (!station) return;

  const stream = station.streamUrl || station.stream_url;
  showToast(`Comprobando stream de ${station.name}...`, 'info');

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 6000);

    const response = await fetch(stream, {
      method: 'GET',
      headers: { 'Range': 'bytes=0-1' },
      signal: controller.signal
    });

    clearTimeout(timeoutId);

    if (response.ok || response.status === 206) {
      showToast(`Stream operativo (${station.name}) - HTTP ${response.status}`, 'success');
    } else {
      showToast(`Aviso: Stream respondió con código HTTP ${response.status}`, 'warning');
    }
  } catch (err) {
    if (err.name === 'AbortError') {
      showToast(`Tiempo de espera agotado al conectar con ${station.name}`, 'warning');
    } else {
      showToast(`Stream con bloqueo CORS o no accesible en navegador (${station.name})`, 'warning');
    }
  }
}

/**
 * Muestra una notificación temporal de tipo Toast en pantalla
 * @param {string} message - Texto del mensaje
 * @param {string} type - 'info', 'success', 'warning', 'error'
 */
function showToast(message, type = 'info') {
  const toast = elements.toast;
  toast.textContent = message;
  toast.className = `toast-notification toast-${type}`;
  toast.style.display = 'block';

  if (window.toastTimeout) {
    clearTimeout(window.toastTimeout);
  }

  window.toastTimeout = setTimeout(() => {
    toast.style.display = 'none';
  }, 3500);
}

/**
 * Abre el modal para añadir una nueva emisora con el formulario en blanco
 */
function openAddModal() {
  elements.formIsEdit.value = 'false';
  elements.modalTitle.textContent = 'Añadir Nueva Emisora';
  elements.inputStationId.disabled = false;
  elements.form.reset();
  elements.inputStationLogo.value = '/logos/radio_default.svg';
  elements.modal.style.display = 'flex';
}

/**
 * Abre el modal para editar una emisora existente rellenando sus datos
 * @param {string} stationId - Identificador único de la emisora
 */
function openEditModal(stationId) {
  const station = state.stations.find(s => String(s.id) === String(stationId));
  if (!station) return;

  elements.formIsEdit.value = 'true';
  elements.modalTitle.textContent = `Editar Emisora: ${station.name}`;
  elements.inputStationId.value = station.id;
  elements.inputStationId.disabled = true;
  elements.inputStationName.value = station.name;
  elements.inputStationGenre.value = station.genre || '';
  elements.inputStationCountry.value = station.country || 'España';
  elements.inputStationStream.value = station.streamUrl || station.stream_url || '';
  elements.inputStationLogo.value = station.logoUrl || station.logo_url || '/logos/radio_default.svg';
  elements.inputStationDesc.value = station.description || '';

  elements.modal.style.display = 'flex';
}

/**
 * Cierra la ventana modal de edición/creación de emisoras
 */
function closeModal() {
  elements.modal.style.display = 'none';
  elements.form.reset();
}

/**
 * Gestiona el envío del formulario para crear o actualizar una emisora en el servidor
 * @param {Event} event - Evento nativo de envío de formulario
 */
async function handleFormSubmit(event) {
  event.preventDefault();

  const isEdit = elements.formIsEdit.value === 'true';
  const stationData = {
    id: elements.inputStationId.value.trim(),
    name: elements.inputStationName.value.trim(),
    genre: elements.inputStationGenre.value.trim() || 'Variedad',
    country: elements.inputStationCountry.value.trim() || 'España',
    streamUrl: elements.inputStationStream.value.trim(),
    stream_url: elements.inputStationStream.value.trim(),
    logoUrl: elements.inputStationLogo.value.trim() || '/logos/radio_default.svg',
    logo_url: elements.inputStationLogo.value.trim() || '/logos/radio_default.svg',
    description: elements.inputStationDesc.value.trim(),
    isFavorite: state.favorites.has(elements.inputStationId.value.trim()),
    favorite: state.favorites.has(elements.inputStationId.value.trim())
  };

  try {
    const url = isEdit ? `/api/stations/${stationData.id}` : '/api/stations';
    const method = isEdit ? 'PUT' : 'POST';

    const response = await fetch(url, {
      method: method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(stationData)
    });

    if (!response.ok) {
      const errData = await response.json().catch(() => ({}));
      throw new Error(errData.error || `HTTP ${response.status}`);
    }

    closeModal();
    showToast(isEdit ? 'Emisora actualizada con éxito' : 'Emisora añadida al catálogo', 'success');
    await loadStations();
  } catch (error) {
    console.error('Error al guardar emisora:', error);
    showToast(`Error al guardar: ${error.message}`, 'error');
  }
}

/**
 * Elimina una emisora del catálogo previa confirmación del usuario
 * @param {string} stationId - Identificador único de la emisora a eliminar
 */
async function deleteStation(stationId) {
  const station = state.stations.find(s => String(s.id) === String(stationId));
  const stationName = station ? station.name : stationId;

  if (!confirm(`¿Estás seguro de que deseas eliminar la emisora "${stationName}"?`)) {
    return;
  }

  try {
    const response = await fetch(`/api/stations/${stationId}`, { method: 'DELETE' });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    state.favorites.delete(String(stationId));
    saveFavoritesToStorage();
    showToast(`Emisora "${stationName}" eliminada`, 'success');
    await loadStations();
  } catch (error) {
    console.error('Error al eliminar emisora:', error);
    showToast(`Error al eliminar: ${error.message}`, 'error');
  }
}

/**
 * Restaura el catálogo de emisoras al estado inicial predeterminado
 */
async function resetCatalog() {
  if (!confirm('¿Restaurar el catálogo original de emisoras? Se desharán las modificaciones locales.')) {
    return;
  }

  try {
    const response = await fetch('/api/reset', { method: 'POST' });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    showToast('Catálogo restaurado al estado original', 'success');
    await loadStations();
  } catch (error) {
    console.error('Error al restaurar catálogo:', error);
    showToast(`Error al restaurar: ${error.message}`, 'error');
  }
}

// Inicializar la aplicación cuando el documento esté completamente cargado
document.addEventListener('DOMContentLoaded', initApp);
