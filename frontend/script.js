document.addEventListener('DOMContentLoaded', () => {
    // Elements - Transcription
    const transcribeBtn = document.getElementById('transcribeBtn');
    const transcribeBtnText = document.getElementById('transcribeBtnText');
    const transcribeLoader = document.getElementById('transcribeLoader');
    const audioFileInput = document.getElementById('audioFileInput');
    const transcriptionResultContainer = document.getElementById('transcriptionResultContainer');
    const transcriptionText = document.getElementById('transcriptionText');
    const txHash = document.getElementById('txHash');
    const txCache = document.getElementById('txCache');

    // Elements - Analysis
    const analyzeBtn = document.getElementById('analyzeBtn');
    const analyzeBtnText = document.getElementById('analyzeBtnText');
    const analyzeLoader = document.getElementById('analyzeLoader');
    const analysisResultContainer = document.getElementById('analysisResultContainer');
    const analysisText = document.getElementById('analysisText');
    const axHash = document.getElementById('axHash');
    const axModel = document.getElementById('axModel');
    const axCache = document.getElementById('axCache');

    let currentAudioHash = null;

    // --- Transcription Logic ---
    transcribeBtn.addEventListener('click', async () => {
        const file = audioFileInput.files[0];

        if (!file) {
            alert('Por favor, selecione um arquivo de áudio primeiro.');
            return;
        }

        setLoadingState(transcribeBtn, transcribeBtnText, transcribeLoader, true);
        transcriptionResultContainer.classList.add('hidden');
        transcriptionText.textContent = '';
        txHash.textContent = '';
        txCache.textContent = '';

        // Reset analysis state when a new audio is transcribed
        currentAudioHash = null;
        analyzeBtn.disabled = true;
        analysisResultContainer.classList.add('hidden');
        analysisText.textContent = '';
        axHash.textContent = '';
        axModel.textContent = '';
        axCache.textContent = '';

        const formData = new FormData();
        formData.append('file', file);

        try {
            const response = await fetch('http://localhost:8080/api/transcriptions', {
                method: 'POST',
                body: formData,
            });

            if (!response.ok) {
                throw new Error(`Erro no servidor: ${response.status} ${response.statusText}`);
            }

            const data = await response.json();

            if (data.text) {
                transcriptionText.textContent = data.text;
                transcriptionResultContainer.classList.remove('hidden');

                // Show hash and cache status
                if (data.audioHash) {
                    txHash.innerHTML = `<div class="info-box"><strong>Hash:</strong> ${data.audioHash}</div>`;
                }
                txCache.innerHTML = `<div class="info-box"><strong>Cache:</strong> ${data.cached ? 'Sim' : 'Não'}</div>`; // Placeholder: need correct variable

                // Enable Analysis button if we have an audioHash
                if (data.audioHash) {
                    currentAudioHash = data.audioHash;
                    analyzeBtn.disabled = false;
                }
            } else {
                throw new Error('O servidor não retornou nenhum texto de transcrição.');
            }

        } catch (error) {
            console.error('Erro na transcrição:', error);
            alert(`Erro: ${error.message}`);
        } finally {
            setLoadingState(transcribeBtn, transcribeBtnText, transcribeLoader, false);
        }
    });

    // --- Analysis Logic ---
    analyzeBtn.addEventListener('click', async () => {
        if (!currentAudioHash) {
            alert('Erro: Hash de áudio não encontrado. Por favor, transcreva o áudio novamente.');
            return;
        }

        setLoadingState(analyzeBtn, analyzeBtnText, analyzeLoader, true);
        analysisResultContainer.classList.add('hidden');
        analysisText.textContent = '';
        axHash.textContent = '';
        axModel.textContent = '';
        axCache.textContent = '';

        try {
            const response = await fetch(`http://localhost:8080/api/transcriptions/${currentAudioHash}/analysis`, {
                method: 'POST',
            });

            if (!response.ok) {
                throw new Error(`Erro no servidor: ${response.status} ${response.statusText}`);
            }

            const data = await response.json();

            if (data.summary) {
                analysisText.textContent = data.summary;
                analysisResultContainer.classList.remove('hidden');
            } else if (data.text) {
                // Some versions might return the summary in a different field
                analysisText.textContent = data.text;
                analysisResultContainer.classList.remove('hidden');
            } else {
                throw new Error('O servidor não retornou nenhum resumo.');
            }

            // Show hash, model, and cache status
            if (data.audioHash) {
                axHash.innerHTML = `<div class="info-box"><strong>Hash:</strong> ${data.audioHash}</div>`;
            }
            if (data.model) {
                axModel.innerHTML = `<div class="info-box"><strong>Modelo:</strong> ${data.model}</div>`;
            }
            axCache.innerHTML = `<div class="info-box"><strong>Cache:</strong> ${data.cached ? 'Sim' : 'Não'}</div>`;

        } catch (error) {
            console.error('Erro na análise:', error);
            alert(`Erro: ${error.message}`);
        } finally {
            setLoadingState(analyzeBtn, analyzeBtnText, analyzeLoader, false);
        }
    });

    function setLoadingState(button, textElement, loaderElement, isLoading) {
        if (isLoading) {
            button.disabled = true;
            textElement.textContent = button.id === 'transcribeBtn' ? 'Transcrevendo...' : 'Analisando...';
            loaderElement.classList.remove('hidden');
        } else {
            button.disabled = false;
            textElement.textContent = button.id === 'transcribeBtn' ? 'Transcrever Áudio' : 'Gerar Resumo AI';
            loaderElement.classList.add('hidden');
        }
    }
});