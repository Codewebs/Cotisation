const express = require('express');
const path = require('path');

const app = express();
const PORT = Number(process.env.PORT) || 3000;
const publicDirectory = path.join(__dirname, 'public');

app.disable('x-powered-by');
app.use(express.static(publicDirectory, { index: 'index.html' }));

const pages = {
    '/': 'search.html',
    '/register': 'register.html',
    '/login': 'login.html',
    '/search': 'search.html',
    '/create-tontine': 'create_tontine.html',
    '/profile': 'profile.html',
    '/tontine-details': 'tontine_details.html',
    '/caisse-details': 'caisse_details.html',
    '/receipt': 'receipt.html',
    '/loans': 'loan_management.html',
    '/record-loan': 'record_loan.html',
    '/repay-loan': 'record_repayment.html',
    '/fines': 'fines_management.html',
    '/create-caisse': 'create_caisse.html',
    '/record-transaction': 'record_transaction.html',
    '/record-expenditure': 'record_expenditure.html',
    '/distribute-pot': 'distribute_pot.html',
    '/journal': 'journal_transactions.html',
    '/join': 'invitation.html',
    '/delete-account': 'delete_account.html'
};

for (const [route, page] of Object.entries(pages)) {
    app.get(route, (_request, response) => {
        response.sendFile(path.join(publicDirectory, page));
    });
}

app.get('/health', (_request, response) => {
    response.json({ status: 'ok', service: 'static-web' });
});

app.listen(PORT, '0.0.0.0', () => {
    console.log(`Cotisation static web client running on http://localhost:${PORT}`);
});
