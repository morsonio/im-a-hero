const output = document.getElementById('output');
const input = document.getElementById('user-input');
const promptElement = document.getElementById('prompt');
const choicesContainer = document.getElementById('choices');

// Maszyna Stanów
let systemState = 'WARNING'; // Zmieniamy startowy stan na Ostrzeżenie
let currentLang = 'PL';

// Dynamiczne generowanie powitania (Skan Otoczenia)
function generateSpookyBootMessage() {
    let lux = 150; // Wartość domyślna dla testów na PC
    let timeSpent = 120;

    if (window.AndroidBridge) {
        lux = window.AndroidBridge.getLuxLevel();
        timeSpent = window.AndroidBridge.getTimeSpentSeconds();
    }

    let minuty = Math.floor(timeSpent / 60);
    let sekundy = timeSpent % 60;
    let czasTekst = minuty > 0 ? `${minuty} min i ${sekundy} sek` : `${sekundy} sekund`;

    let lightContext = "";
    if (lux < 10) {
        lightContext = `Siedzisz w ciemnościach [${lux.toFixed(1)} Lux]. Idealne warunki dla Łupieżcy Umysłów.`;
    } else if (lux < 100) {
        lightContext = `Półmrok [${lux.toFixed(1)} Lux] nie ukryje Twojej pętli dopaminowej.`;
    } else {
        lightContext = `Ostre światło [${lux.toFixed(1)} Lux] razi w oczy, a Ty wciąż patrzysz w ten sam punkt.`;
    }

    return `[SKANOWANIE BIOMETRYCZNE...]\nZGODNOŚĆ POTWIERDZONA: OPERATOR 011.\n\nOSTRZEŻENIE: Przewijasz tę samą rolkę już od ${czasTekst}.\n${lightContext}\n\nOko z The Upside Down już na ciebie spogląda. Masz krótkie okno na ucieczkę.\nWpisz 'exit', by grzecznie wyłączyć aplikację, lub 'init', by wejść do Kuźni i stawić temu czoła.`;
}

// Dane zebrane z profilowania
let profileData = {
    energy: 0,      // 1 (Safe Mode) do 5 (Overheating)
    directive: '',  // 'lawful' lub 'chaotic'
    mmpi_lie: false // Czy pacjent oszukał system?
};

const i18n = {
    'PL': {
        boot: "[SKANOWANIE BIOMETRYCZNE...]\nZGODNOŚĆ POTWIERDZONA: OPERATOR 011.\n\nOSTRZEŻENIE: Wykryto próbę kradzieży czasu. Algorytmy zewnętrze zablokowane.\nUrządzenie w trybie awaryjnym. Wpisz 'init', aby rozpocząć rekalibrację.",

        q1_text: "--- ETAP 1: SKANOWANIE RDZENIA ---\nOkreśl aktualny poziom przeciążenia układu nerwowego (1-5):",
        q1_low: "[1] ZAMROŻENIE (Pustka)",
        q1_high: "[5] FURIA (Przebodźcowanie)",

        q2_text: "--- ETAP 2: DYREKTYWA IDEALNEGO JA ---\nUkład dopaminowy uszkodzony. Jaka jest Twoja taktyka obronna?",
        q2_lawful: "[1] ZBUDUJ ZAPORĘ (Zasady, dyscyplina, struktura)",
        q2_chaotic: "[2] ZBURZ SYSTEM (Hakerstwo, chaos, skróty)",

        q3_text: "--- ETAP 3: WERYFIKACJA SPÓJNOŚCI ---\nReceptory domagają się powrotu do aplikacji. Czy odczuwasz nagły spadek zasilania i potrzebujesz stymulanta?",
        q3_yes: "[1] TAK (Jestem wyczerpany/znudzony)",
        q3_no: "[2] NIE (Roznosi mnie energia/frustracja)",

        mmpi_trap: "\n!!! BŁĄD SPÓJNOŚCI MMPI !!!\nOszukujesz Głównego Inżyniera. Twój pierwotny skan energii przeczy Twoim obecnym słowom.\nKŁAMSTWO WYKRYTE. NAKŁADAM ŁATKĘ KARNĄ.",

        avatar_architect: "WYNIK: [AWATAR ARCHITEKT]\nZłość przekuta w fundamenty. Błąd zamieniony w regułę.",
        avatar_hacker: "WYNIK: [AWATAR HAKER]\nFrustracja łamie bariery. Wykorzystujesz błędy systemu dla własnej korzyści.",
        avatar_ghost: "WYNIK: [AWATAR DUCH]\nZanik napięcia. Odcinasz emocje, stajesz się niewidzialny dla algorytmów.",
        avatar_glitch: "WYNIK: [AWATAR GLITCH]\nBrak oporu. Płyniesz z chaosem, nie dając się uchwycić.",

        unlocked: "\nREKALIBRACJA ZAKOŃCZONA.\nNowy patch zainstalowany w oprogramowaniu mózgu.\nWpisz 'bypass', aby wrócić do symulacji."
    }
};

document.body.addEventListener('click', () => {
    if (systemState !== 'PROFILING_ENERGY' && systemState !== 'PROFILING_DIRECTIVE' && systemState !== 'PROFILING_MMPI') {
        input.focus();
    }
});

input.addEventListener('keydown', function (e) {
    if (e.key === 'Enter') {
        const command = this.value.trim().toLowerCase();
        if (command === '') return;

        appendOutput(`${promptElement.innerText} ${command}`, '#0f0');
        this.value = '';
        processCommand(command);
    }
});

function appendOutput(text, color = '#0f0', cssClass = '') {
    const line = document.createElement('div');
    line.style.color = color;
    line.style.marginTop = '10px';
    if (cssClass) line.className = cssClass;
    line.innerHTML = text.replace(/\n/g, '<br>');
    output.appendChild(line);
    window.scrollTo(0, document.body.scrollHeight);
}

// Funkcja pomocnicza: wstrzykiwanie klikalnych bloków (Retro Przyciski)
function renderChoices(htmlContent) {
    choicesContainer.innerHTML = htmlContent;
    window.scrollTo(0, document.body.scrollHeight);
    input.blur(); // Ukryj klawiaturę na czas klikania w przyciski
}

function clearChoices() {
    choicesContainer.innerHTML = '';
    input.focus(); // Przywróć klawiaturę
}

// Globalna funkcja dla kliknięć w przyciski z HTML
window.submitChoice = function (value) {
    appendOutput(`${promptElement.innerText} [Wprowadzono: ${value}]`, '#0f0');
    processCommand(value);
};

function processCommand(cmd) {
    const t = i18n[currentLang];

    if (systemState === 'WARNING') {
        if (cmd === 'exit') {
            systemState = 'EJECTING'; // Blokujemy wpisywanie innych komend
            appendOutput("Protokół ewakuacji zainicjowany.", '#55ff55');
            appendOutput("Katapultowanie do bezpiecznej strefy za: 3...", '#ffaa00');

            setTimeout(() => appendOutput("2...", '#ffaa00'), 1000);
            setTimeout(() => appendOutput("1...", '#ffaa00'), 2000);
            setTimeout(() => {
                appendOutput("Zrzut!", '#ff0000');
                if (window.AndroidBridge) {
                    // Najpierw wyrzucamy pacjenta na pulpit telefonu
                    window.AndroidBridge.forceHome();
                    // Pół sekundy później zdejmujemy nasz czarny ekran
                    setTimeout(() => window.AndroidBridge.closeTerminal(), 500);
                }
            }, 3000);

        } else if (cmd === 'init') {
            systemState = 'PROFILING_ENERGY';
            appendOutput("--- ETAP 1: SKANOWANIE RDZENIA ---\nOkreśl aktualny poziom przeciążenia układu nerwowego (1-5):", '#55aaff');

            renderChoices(`
                <div class="term-scale">
                    <div class="scale-tick" onclick="submitChoice('1')">1<br>❄️</div>
                    <div class="scale-tick" onclick="submitChoice('2')">2</div>
                    <div class="scale-tick" onclick="submitChoice('3')">3</div>
                    <div class="scale-tick" onclick="submitChoice('4')">4</div>
                    <div class="scale-tick" onclick="submitChoice('5')">5<br>🔥</div>
                </div>
                <div style="display:flex; justify-content:space-between; font-size:0.8rem;">
                    <span>[ZAMROŻENIE]</span><span>[FURIA]</span>
                </div>
            `);
        } else {
            appendOutput("Błędna komenda. Czas ucieka. Wpisz 'exit' lub 'init'.", '#aaa');
        }
        return;
    }

    if (systemState === 'PROFILING_ENERGY') {
        const val = parseInt(cmd);
        if (val >= 1 && val <= 5) {
            profileData.energy = val;
            clearChoices();

            systemState = 'PROFILING_DIRECTIVE';
            appendOutput(t.q2_text, '#55aaff');
            renderChoices(`
                <button class="term-btn" onclick="submitChoice('1')">${t.q2_lawful}</button>
                <button class="term-btn" onclick="submitChoice('2')">${t.q2_chaotic}</button>
            `);
        } else {
            appendOutput("Błąd kalibracji. Wybierz wartość od 1 do 5.", '#aaa');
        }
        return;
    }

    if (systemState === 'PROFILING_DIRECTIVE') {
        if (cmd === '1' || cmd === '2') {
            profileData.directive = (cmd === '1') ? 'lawful' : 'chaotic';
            clearChoices();

            systemState = 'PROFILING_MMPI';
            appendOutput(t.q3_text, '#55aaff');
            renderChoices(`
                <button class="term-btn" onclick="submitChoice('1')">${t.q3_yes}</button>
                <button class="term-btn" onclick="submitChoice('2')">${t.q3_no}</button>
            `);
        } else {
            appendOutput("Błąd kalibracji. Wpisz '1' lub '2'.", '#aaa');
        }
        return;
    }

    if (systemState === 'PROFILING_MMPI') {
        if (cmd === '1' || cmd === '2') {
            clearChoices();
            let wantsStimulus = (cmd === '1');

            // --- PUŁAPKA MMPI (TEST SPÓJNOŚCI) ---
            // Jeśli ustawił suwak na Furię (4,5), ale mówi, że czuje "spadek zasilania/znudzenie" -> KŁAMIE
            // Jeśli ustawił suwak na Pustkę (1,2), ale mówi, że "rozsadza go energia" -> KŁAMIE
            if ((profileData.energy >= 4 && wantsStimulus) || (profileData.energy <= 2 && !wantsStimulus)) {
                profileData.mmpi_lie = true;
                appendOutput(t.mmpi_trap, '#ff0000', 'fatal-error'); // Odpala CSS Glitch
                // Tu w przyszłości Kotlin może odpalić wibrator za karę!
            } else {
                appendOutput("Spójność zweryfikowana. Poziom szczerości: Zadowalający.", '#55ff55');
            }

            // ANALIZA AWATARA (Kompilacja wyników)
            setTimeout(() => { calculateAvatar(t); }, 1500);

        } else {
            appendOutput("Błąd weryfikacji. Wpisz '1' lub '2'.", '#aaa');
        }
        return;
    }

    if (systemState === 'UNLOCKED') {
        if (cmd === 'bypass') {
            executeBypass();
        }
    }
}

function calculateAvatar(t) {
    let resultText = "";
    let isHighEnergy = profileData.energy >= 3;

    if (profileData.directive === 'lawful' && isHighEnergy) resultText = t.avatar_architect;
    if (profileData.directive === 'chaotic' && isHighEnergy) resultText = t.avatar_hacker;
    if (profileData.directive === 'lawful' && !isHighEnergy) resultText = t.avatar_ghost;
    if (profileData.directive === 'chaotic' && !isHighEnergy) resultText = t.avatar_glitch;

    appendOutput("\n" + resultText, '#ffff55');
    appendOutput(t.unlocked, '#aaa');
    systemState = 'UNLOCKED';
}

function executeBypass() {
    appendOutput("SYSTEM OVERRIDE INITIATED. Powrót do rzeczywistości...", '#55ff55');
    setTimeout(() => {
        if (window.AndroidBridge) {
            window.AndroidBridge.closeTerminal();
            // Twardy reset dla następnego ataku
            systemState = 'LANGUAGE_SELECT';
            output.innerHTML = '';
            clearChoices();
            appendOutput(i18n['PL'].boot, '#ff5555');
            systemState = 'LOCKED';
        }
    }, 1500);
}

// Odpalenie od razu polskiego komunikatu startowego
setTimeout(() => {
    output.innerHTML = '';
    appendOutput(generateSpookyBootMessage(), '#ff5555');
}, 500);