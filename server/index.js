require('dotenv').config();
const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const { v4: uuidv4 } = require('uuid');
const { Sequelize, DataTypes, Op } = require('sequelize');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(bodyParser.json());
app.use(express.static('public')); // Pour les interfaces Web

// --- DATABASE INITIALIZATION ---
const sequelize = new Sequelize(
    process.env.DB_NAME,
    process.env.DB_USER,
    process.env.DB_PASS,
    {
        host: process.env.DB_HOST,
        port: process.env.DB_PORT,
        dialect: 'mysql',
        logging: false
    }
);

// --- MODELS ---
const User = sequelize.define('User', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    nom: { type: DataTypes.STRING, allowNull: false },
    phone: { type: DataTypes.STRING, allowNull: false, unique: true },
    matricule: { type: DataTypes.STRING, unique: true },
    email: { type: DataTypes.STRING },
    pinHash: { type: DataTypes.STRING, allowNull: false },
    pinSalt: { type: DataTypes.STRING, defaultValue: 'static_salt' },
    isActive: { type: DataTypes.BOOLEAN, defaultValue: true },
    isVerified: { type: DataTypes.BOOLEAN, defaultValue: true }
});

const Tontine = sequelize.define('Tontine', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    libelle: { type: DataTypes.STRING, allowNull: false },
    exercice: { type: DataTypes.STRING },
    dateDebut: { type: DataTypes.STRING },
    dateFin: { type: DataTypes.STRING },
    updatedAt: { type: DataTypes.BIGINT }
});

const Member = sequelize.define('Member', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    tontineId: { type: DataTypes.UUID, allowNull: false },
    userId: { type: DataTypes.UUID },
    nomSaisi: { type: DataTypes.STRING, allowNull: false },
    phone: { type: DataTypes.STRING },
    role: { type: DataTypes.STRING, defaultValue: 'MEMBRE' },
    isInitiator: { type: DataTypes.BOOLEAN, defaultValue: false },
    joinedAt: { type: DataTypes.BIGINT, defaultValue: Date.now() }
});

const Caisse = sequelize.define('Caisse', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    tontineId: { type: DataTypes.UUID, allowNull: false },
    type: { type: DataTypes.STRING },
    libelle: { type: DataTypes.STRING },
    montantFixe: { type: DataTypes.BIGINT },
    montantMinimum: { type: DataTypes.BIGINT },
    regulariteImposee: { type: DataTypes.BOOLEAN, defaultValue: false },
    redistribution: { type: DataTypes.BOOLEAN, defaultValue: false },
    ordreRedistribution: { type: DataTypes.INTEGER },
    tauxInteret: { type: DataTypes.DOUBLE },
    typeInteret: { type: DataTypes.STRING }, // SIMPLE, COMPOSE
    penaliteRetardFixe: { type: DataTypes.BIGINT },
    penaliteEchecFixe: { type: DataTypes.BIGINT },
    dateLimiteJour: { type: DataTypes.INTEGER },
    ordreFige: { type: DataTypes.BOOLEAN, defaultValue: false },
    possibilitePret: { type: DataTypes.BOOLEAN, defaultValue: false },
    dureeCycles: { type: DataTypes.INTEGER },
    plafondPret: { type: DataTypes.BIGINT },
    isBasique: { type: DataTypes.BOOLEAN, defaultValue: false },
    updatedAt: { type: DataTypes.BIGINT }
});

const Cycle = sequelize.define('Cycle', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    caisseId: { type: DataTypes.UUID, allowNull: false },
    numeroCycle: { type: DataTypes.INTEGER, allowNull: false },
    dateDebut: { type: DataTypes.BIGINT },
    dateFin: { type: DataTypes.BIGINT },
    estCloture: { type: DataTypes.BOOLEAN, defaultValue: false }
});

const Tour = sequelize.define('Tour', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    cycleId: { type: DataTypes.UUID, allowNull: false },
    numeroTour: { type: DataTypes.INTEGER, allowNull: false },
    dateTour: { type: DataTypes.BIGINT },
    estTermine: { type: DataTypes.BOOLEAN, defaultValue: false }
});

const Transaction = sequelize.define('Transaction', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    caisseId: { type: DataTypes.UUID, allowNull: false },
    memberId: { type: DataTypes.UUID, allowNull: false },
    cycleId: { type: DataTypes.UUID, allowNull: false },
    montant: { type: DataTypes.BIGINT, allowNull: false },
    type: { type: DataTypes.STRING, allowNull: false }, // COTISATION, PRET, REMBOURSEMENT, AMENDE, PENALITE, SORTIE
    motif: { type: DataTypes.STRING },
    dateTransaction: { type: DataTypes.BIGINT, defaultValue: Date.now() }
});

const Pret = sequelize.define('Pret', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    transactionId: { type: DataTypes.UUID, allowNull: false },
    memberId: { type: DataTypes.UUID, allowNull: false },
    montantInitial: { type: DataTypes.BIGINT, allowNull: false },
    montantRestant: { type: DataTypes.BIGINT, allowNull: false },
    tauxInteret: { type: DataTypes.DOUBLE },
    dateEcheance: { type: DataTypes.BIGINT },
    statut: { type: DataTypes.STRING, defaultValue: 'ACTIF' } // ACTIF, SOLDE
});

const SortieCaisse = sequelize.define('SortieCaisse', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    caisseId: { type: DataTypes.UUID, allowNull: false },
    montant: { type: DataTypes.BIGINT, allowNull: false },
    motif: { type: DataTypes.STRING },
    dateSortie: { type: DataTypes.BIGINT, defaultValue: Date.now() }
});

// --- RELATIONSHIPS ---
Tontine.hasMany(Member, { foreignKey: 'tontineId', as: 'members' });
Tontine.hasMany(Caisse, { foreignKey: 'tontineId', as: 'caisses' });
Member.belongsTo(Tontine, { foreignKey: 'tontineId' });
Caisse.belongsTo(Tontine, { foreignKey: 'tontineId' });

Caisse.hasMany(Cycle, { foreignKey: 'caisseId', as: 'cycles' });
Cycle.belongsTo(Caisse, { foreignKey: 'caisseId' });

Cycle.hasMany(Tour, { foreignKey: 'cycleId', as: 'tours' });
Tour.belongsTo(Cycle, { foreignKey: 'cycleId' });

Caisse.hasMany(Transaction, { foreignKey: 'caisseId', as: 'transactions' });
Transaction.belongsTo(Caisse, { foreignKey: 'caisseId' });

Member.hasMany(Transaction, { foreignKey: 'memberId', as: 'transactions' });
Transaction.belongsTo(Member, { foreignKey: 'memberId' });

Cycle.hasMany(Transaction, { foreignKey: 'cycleId', as: 'transactions' });
Transaction.belongsTo(Cycle, { foreignKey: 'cycleId' });

Transaction.hasOne(Pret, { foreignKey: 'transactionId', as: 'pret' });
Pret.belongsTo(Transaction, { foreignKey: 'transactionId' });

Member.hasMany(Pret, { foreignKey: 'memberId', as: 'prets' });
Pret.belongsTo(Member, { foreignKey: 'memberId' });

Caisse.hasMany(SortieCaisse, { foreignKey: 'caisseId', as: 'sorties' });
SortieCaisse.belongsTo(Caisse, { foreignKey: 'caisseId' });

// Sync Database
sequelize.sync().then(() => console.log("Database & tables synced with Android schema level!"));

// Helper
const generateMatricule = () => 'TON-' + Math.floor(100000 + Math.random() * 900000);

// --- ROOT & HEALTH CHECK ---
app.get('/', (req, res) => {
    res.send(`
        <!DOCTYPE html>
        <html lang="fr">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Cotisation API - Dashboard</title>
            <script src="https://cdn.tailwindcss.com"></script>
        </head>
        <body class="bg-gray-900 text-white min-h-screen flex items-center justify-center p-6">
            <div class="max-w-2xl w-full bg-gray-800 rounded-3xl shadow-2xl border border-gray-700 p-10 text-center">
                <div id="user-welcome" class="hidden mb-6 flex items-center justify-center gap-3 bg-cyan-500/10 py-3 px-6 rounded-2xl border border-cyan-500/20">
                    <div class="w-8 h-8 bg-cyan-500 rounded-full flex items-center justify-center text-xs font-black text-gray-900">
                        <span id="user-initial">U</span>
                    </div>
                    <span class="text-sm font-bold text-cyan-400">Bienvenue, <span id="user-name">Membre</span></span>
                </div>

                <div id="default-icon" class="mb-6 inline-flex p-4 bg-cyan-500/10 rounded-2xl">
                    <svg class="w-12 h-12 text-cyan-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
                    </svg>
                </div>
                <h1 class="text-4xl font-black mb-4 text-white uppercase tracking-tight">COTISATION <span class="text-cyan-400">SERVER</span></h1>
                <p class="text-gray-400 mb-10 text-lg">Le moteur de votre tontine numérique est en marche.</p>

                <div id="auth-links" class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-left">
                    <a href="/register" class="bg-gray-700/30 hover:bg-gray-700/50 p-6 rounded-2xl border border-gray-600 hover:border-cyan-500/50 transition group">
                        <h3 class="font-bold text-cyan-400 mb-1 group-hover:translate-x-1 transition tracking-wide">INSCRIPTION</h3>
                        <p class="text-xs text-gray-500">Créer un nouveau compte utilisateur</p>
                    </a>
                    <a href="/login" class="bg-gray-700/30 hover:bg-gray-700/50 p-6 rounded-2xl border border-gray-600 hover:border-cyan-500/50 transition group">
                        <h3 class="font-bold text-cyan-400 mb-1 group-hover:translate-x-1 transition tracking-wide">CONNEXION</h3>
                        <p class="text-xs text-gray-500">Accéder au tableau de bord membre</p>
                    </a>
                </div>

                <div id="main-links" class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-left mt-4">
                    <a href="/profile" id="link-profile" class="bg-gray-700/30 hover:bg-gray-700/50 p-6 rounded-2xl border border-gray-600 hover:border-cyan-500/50 transition group">
                        <h3 class="font-bold text-cyan-400 mb-1 group-hover:translate-x-1 transition tracking-wide">MON PROFIL</h3>
                        <p class="text-xs text-gray-500">Gérer mes tontines et infos</p>
                    </a>
                    <a href="/search" class="bg-gray-700/30 hover:bg-gray-700/50 p-6 rounded-2xl border border-gray-600 hover:border-cyan-500/50 transition group">
                        <h3 class="font-bold text-cyan-400 mb-1 group-hover:translate-x-1 transition tracking-wide">RECHERCHE</h3>
                        <p class="text-xs text-gray-500">Explorer les tontines existantes</p>
                    </a>
                    <a href="/create-tontine" id="link-create" class="bg-gray-700/30 hover:bg-gray-700/50 p-6 rounded-2xl border border-gray-600 hover:border-cyan-500/50 transition group">
                        <h3 class="font-bold text-cyan-400 mb-1 group-hover:translate-x-1 transition tracking-wide">CRÉATION</h3>
                        <p class="text-xs text-gray-500">Lancer une nouvelle tontine</p>
                    </a>
                    <a href="/health" class="bg-gray-700/30 hover:bg-gray-700/50 p-6 rounded-2xl border border-gray-600 hover:border-emerald-500/50 transition group">
                        <h3 class="font-bold text-emerald-400 mb-1 group-hover:translate-x-1 transition tracking-wide">SANTÉ API</h3>
                        <p class="text-xs text-gray-500">État du système et statistiques</p>
                    </a>
                </div>

                <script>
                    document.addEventListener('DOMContentLoaded', () => {
                        const user = JSON.parse(localStorage.getItem('currentUser'));
                        const authLinks = document.getElementById('auth-links');
                        const welcomeBox = document.getElementById('user-welcome');
                        const defaultIcon = document.getElementById('default-icon');
                        const linkProfile = document.getElementById('link-profile');
                        const linkCreate = document.getElementById('link-create');

                        if (user) {
                            authLinks.classList.add('hidden');
                            defaultIcon.classList.add('hidden');
                            welcomeBox.classList.remove('hidden');
                            document.getElementById('user-name').innerText = user.nom;
                            document.getElementById('user-initial').innerText = user.nom.charAt(0).toUpperCase();
                        } else {
                            linkProfile.classList.add('hidden');
                            linkCreate.classList.add('hidden');
                        }
                    });
                </script>

                <div class="mt-12 pt-8 border-t border-gray-700 flex justify-between items-center text-xs text-gray-500 font-mono">
                    <span>MODE: DEVELOPMENT</span>
                    <span>v1.1.0-MYSQL</span>
                </div>
            </div>
        </body>
        </html>
    `);
});

app.get('/health', async (req, res) => {
    const userCount = await User.count();
    const tontineCount = await Tontine.count();
    const caisseCount = await Caisse.count();
    const memberCount = await Member.count();
    const transactionCount = await Transaction.count();
    res.json({
        status: 'UP',
        users: userCount,
        tontines: tontineCount,
        caisses: caisseCount,
        members: memberCount,
        transactions: transactionCount
    });
});

// --- API ENDPOINTS ---

app.post('/users/register', async (req, res) => {
    try {
        const { nom, phone, pin } = req.body;
        const user = await User.create({
            nom, phone, pinHash: pin, matricule: generateMatricule()
        });
        res.status(201).json(user);
    } catch (e) {
        res.status(400).json({ error: e.message });
    }
});

app.post('/users/login', async (req, res) => {
    try {
        const { phone, pin } = req.body;
        const user = await User.findOne({ where: { phone, pinHash: pin } });
        if (!user) return res.status(401).json({ error: 'Identifiants invalides.' });
        res.json(user);
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

app.get('/users/search', async (req, res) => {
    try {
        const { phone } = req.query;
        const user = await User.findOne({ where: { phone } });
        if (!user) return res.status(404).json({ error: 'Utilisateur non trouvé' });
        res.json(user);
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

app.post('/tontines', async (req, res) => {
    try {
        const { libelle, exercice, dateDebut, dateFin, creatorId } = req.body;
        const tontine = await Tontine.create({
            libelle, exercice, dateDebut, dateFin, updatedAt: Date.now()
        });

        if (creatorId) {
            const user = await User.findByPk(creatorId);
            if (user) {
                await Member.create({
                    tontineId: tontine.id,
                    userId: user.id,
                    nomSaisi: user.nom,
                    phone: user.phone,
                    role: 'INITIATEUR',
                    isInitiator: true
                });
            }
        }
        res.status(201).json(tontine);
    } catch (e) {
        res.status(400).json({ error: e.message });
    }
});

app.get('/tontines/search', async (req, res) => {
    try {
        const { query } = req.query;
        const results = await Tontine.findAll({
            where: { libelle: { [Op.like]: `%${query}%` } }
        });
        res.json(results);
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

app.get('/tontines/:id', async (req, res) => {
    try {
        const tontine = await Tontine.findByPk(req.params.id, {
            include: [
                { model: Member, as: 'members' },
                { model: Caisse, as: 'caisses' }
            ]
        });
        if (!tontine) return res.status(404).json({ error: 'Tontine non trouvée' });
        res.json(tontine);
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

app.get('/users/:id/tontines', async (req, res) => {
    try {
        const { id } = req.params;
        const results = await Tontine.findAll({
            include: [{
                model: Member,
                as: 'members',
                where: { userId: id }
            }]
        });
        res.json(results);
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

// --- CAISSE ENDPOINTS ---
app.get('/caisses/:id', async (req, res) => {
    try {
        const caisse = await Caisse.findByPk(req.params.id, {
            include: [
                { model: Cycle, as: 'cycles' },
                { model: Transaction, as: 'transactions' }
            ]
        });
        res.json(caisse);
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

// --- WEB INTERFACES (HTML ROUTES) ---
app.get('/register', (req, res) => res.sendFile(path.join(__dirname, 'public', 'register.html')));
app.get('/login', (req, res) => res.sendFile(path.join(__dirname, 'public', 'login.html')));
app.get('/search', (req, res) => res.sendFile(path.join(__dirname, 'public', 'search.html')));
app.get('/create-tontine', (req, res) => res.sendFile(path.join(__dirname, 'public', 'create_tontine.html')));
app.get('/profile', (req, res) => res.sendFile(path.join(__dirname, 'public', 'profile.html')));
app.get('/tontine-details', (req, res) => res.sendFile(path.join(__dirname, 'public', 'tontine_details.html')));

app.listen(PORT, '0.0.0.0', () => {
    console.log(`Cotisation Server is running on http://localhost:${PORT}`);
});
