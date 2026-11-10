(function () {
    const config = window.COTISATION_SUPABASE_CONFIG || {};
    if (!window.supabase || !config.url || !config.anonKey ||
        config.url.includes('YOUR_PROJECT_ID') ||
        config.anonKey.includes('YOUR_SUPABASE')) {
        console.error('Configure the public Supabase URL and anon/publishable key in js/supabase-config.js.');
        return;
    }

    const client = window.supabase.createClient(config.url, config.anonKey);
    window.db = client;

    window.getCurrentUser = async function () {
        const { data: { user }, error } = await client.auth.getUser();
        if (error) throw error;
        if (!user) return null;

        const { data: profile, error: profileError } = await client
            .from('profiles').select('*').eq('id', user.id).maybeSingle();
        if (profileError) throw profileError;

        const current = Object.assign({}, profile || {}, {
            id: user.id,
            email: user.email,
            nom: (profile && profile.nom) || user.user_metadata?.nom || user.user_metadata?.name || ''
        });
        localStorage.setItem('currentUser', JSON.stringify(current));
        return current;
    };

    window.escapeHtml = function (value) {
        return String(value == null ? '' : value).replace(/[&<>"']/g, function (char) {
            return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char];
        });
    };
})();
