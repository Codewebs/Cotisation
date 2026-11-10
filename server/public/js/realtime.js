(function () {
    if (!window.db || !window.getCurrentUser) return;

    async function subscribe() {
        const user = await window.getCurrentUser().catch((error) => {
            console.error('Unable to verify the current Supabase session for realtime:', error);
            return null;
        });
        if (!user) return;

        window.db.channel(`user-notifications-${user.id}`)
            .on('postgres_changes', {
                event: 'INSERT',
                schema: 'public',
                table: 'notifications',
                filter: `user_id=eq.${user.id}`
            }, ({ new: notification }) => {
                showToast(notification);
                window.dispatchEvent(new CustomEvent('new_notification', { detail: notification }));
            })
            .subscribe();

        const { data: memberships, error } = await window.db.from('members')
            .select('tontine_id').eq('user_id', user.id);
        if (error) {
            console.error('Unable to load realtime group subscriptions:', error);
            return;
        }

        memberships.forEach(({ tontine_id: tontineId }) => {
            window.db.channel(`join-requests-${user.id}-${tontineId}`)
                .on('postgres_changes', {
                    event: '*',
                    schema: 'public',
                    table: 'join_requests',
                    filter: `tontine_id=eq.${tontineId}`
                }, (payload) => {
                    window.dispatchEvent(new CustomEvent('join_request_change', {
                        detail: { tontineId, ...payload }
                    }));
                })
                .subscribe();
        });
    }

    function showToast(notification) {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'fixed top-4 right-4 z-50 flex flex-col gap-3 max-w-sm w-full pointer-events-none';
            document.body.appendChild(container);
        }

        const toast = document.createElement('button');
        toast.type = 'button';
        toast.className = 'pointer-events-auto bg-gray-900 border-2 border-cyan-500 text-white p-4 rounded-2xl shadow-2xl flex flex-col gap-1 transition-all duration-300 opacity-0 cursor-pointer hover:border-cyan-400 text-left';

        const label = document.createElement('span');
        label.className = 'text-[10px] font-black tracking-wider uppercase text-cyan-400';
        label.textContent = 'TEMPS RÉEL';
        const title = document.createElement('span');
        title.className = 'text-sm font-bold text-white mt-1';
        title.textContent = notification.title || 'Notification';
        const message = document.createElement('span');
        message.className = 'text-xs text-gray-300';
        message.textContent = notification.message || '';
        toast.append(label, title, message);

        toast.addEventListener('click', () => {
            const referenceId = notification.reference_id;
            if (notification.type === 'RECEIPT' && referenceId) {
                window.location.href = `/receipt?txId=${encodeURIComponent(referenceId)}`;
            } else if ((notification.type === 'CYCLE_OPEN' || notification.type === 'CYCLE_CLOSE') && referenceId) {
                window.location.href = `/caisse-details?id=${encodeURIComponent(referenceId)}`;
            } else if (referenceId) {
                window.location.href = `/tontine-details?id=${encodeURIComponent(referenceId)}`;
            }
        });
        container.appendChild(toast);
        requestAnimationFrame(() => toast.classList.remove('opacity-0'));
        setTimeout(() => {
            toast.classList.add('opacity-0');
            setTimeout(() => toast.remove(), 300);
        }, 6000);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', subscribe);
    } else {
        subscribe();
    }
})();
