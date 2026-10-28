/**
 * Integração Frontend ↔ Backend APS
 *
 * A chave da OpenWeatherMap NÃO é exposta aqui.
 * Todas as chamadas meteorológicas passam pelo proxy do backend Java
 * em http://localhost:8080/proxy/* — o backend guarda a API key com segurança
 * via variável de ambiente OPENWEATHER_API_KEY.
 *
 * Para trocar a porta, defina window.BACKEND_URL antes de carregar este script.
 */
window.BACKEND_URL = window.BACKEND_URL || 'http://localhost:8080';
