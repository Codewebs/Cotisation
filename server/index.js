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

// --- LOGGING MIDDLEWARE ---
app.use((req, res, next) => {
    const start = Date.now();
    console.log(`\n[${new Date().toISOString()}] >>> ${req.method} ${req.originalUrl}`);
    if (Object.keys(req.params).length) console.log(`Params: ${JSON.stringify(req.params)}`);
    if (Object.keys(req.query).length) console.log(`Query: ${JSON.stringify(req.query)}`);
    if (Object.keys(req.body).length) console.log(`Body: ${JSON.stringify(req.body)}`);

    res.on('finish', () => {
        const duration = Date.now() - start;
        console.log(`[${new Date().toISOString()}] <<< ${res.statusCode} (${duration}ms)`);
    });
    next();
});

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
    creatorId: { type: DataTypes.UUID }, // L'initiateur du cercle
    isOnline: { type: DataTypes.BOOLEAN, defaultValue: true },
    updatedAt: { type: DataTypes.BIGINT }
}, { timestamps: false });

const Member = sequelize.define('Member', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    tontineId: { type: DataTypes.UUID, allowNull: false },
    userId: { type: DataTypes.UUID },
    nomSaisi: { type: DataTypes.STRING, allowNull: false },
    phone: { type: DataTypes.STRING },
    role: { type: DataTypes.STRING, defaultValue: 'MEMBRE' },
    isInitiator: { type: DataTypes.BOOLEAN, defaultValue: false },
    joinedAt: { type: DataTypes.BIGINT, defaultValue: () => Date.now() }
}, { timestamps: false });

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
    fraisCaisse: { type: DataTypes.BIGINT },
    statut: { type: DataTypes.STRING, defaultValue: 'ACTIVE' },
    respecterFrequenceStrictement: { type: DataTypes.BOOLEAN, defaultValue: false },
    ordreFige: { type: DataTypes.BOOLEAN, defaultValue: false },
    possibilitePret: { type: DataTypes.BOOLEAN, defaultValue: false },
    autoriserPretSurAmendes: { type: DataTypes.BOOLEAN, defaultValue: false },
    frequence: { type: DataTypes.STRING, defaultValue: 'MENSUEL' },
    dureeCycles: { type: DataTypes.INTEGER },
    plafondPret: { type: DataTypes.BIGINT },
    isBasique: { type: DataTypes.BOOLEAN, defaultValue: false },
    updatedAt: { type: DataTypes.BIGINT }
}, { timestamps: false });

const CaisseParticipant = sequelize.define('CaisseParticipant', {
    caisseId: { type: DataTypes.UUID, primaryKey: true },
    memberId: { type: DataTypes.UUID, primaryKey: true },
    ordrePassage: { type: DataTypes.INTEGER, defaultValue: 0 },
    nombreParts: { type: DataTypes.INTEGER, defaultValue: 1 }
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
    beneficiaryId: { type: DataTypes.UUID }, // Membre qui reçoit la cagnotte
    numeroTour: { type: DataTypes.INTEGER, allowNull: false },
    dateTour: { type: DataTypes.BIGINT },
    estTermine: { type: DataTypes.BOOLEAN, defaultValue: false }
});

const Transaction = sequelize.define('Transaction', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    caisseId: { type: DataTypes.UUID, allowNull: false },
    memberId: { type: DataTypes.UUID, allowNull: false },
    cycleId: { type: DataTypes.UUID, allowNull: false },
    validatedById: { type: DataTypes.UUID }, // ID de l'administrateur qui a validé
    montant: { type: DataTypes.BIGINT, allowNull: false },
    type: { type: DataTypes.STRING, allowNull: false }, // COTISATION, PRET, REMBOURSEMENT, AMENDE, PENALITE, SORTIE
    motif: { type: DataTypes.STRING },
    dateTransaction: { type: DataTypes.BIGINT, defaultValue: Date.now() }
});

const Pret = sequelize.define('Pret', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    caisseId: { type: DataTypes.UUID, allowNull: false },
    memberId: { type: DataTypes.UUID },
    transactionId: { type: DataTypes.UUID }, // Clé étrangère vers la transaction de décaissement
    montantPrincipal: { type: DataTypes.BIGINT, allowNull: false },
    montantInterets: { type: DataTypes.BIGINT, defaultValue: 0 },
    montantRestant: { type: DataTypes.BIGINT },
    tauxInteret: { type: DataTypes.DOUBLE, defaultValue: 0 },
    typeInteret: { type: DataTypes.STRING },
    datePret: { type: DataTypes.BIGINT, defaultValue: Date.now() },
    dateEcheance: { type: DataTypes.BIGINT },
    statut: { type: DataTypes.STRING, defaultValue: 'PENDING' },
    nomEmprunteurExterne: { type: DataTypes.STRING },
    cniEmprunteurExterne: { type: DataTypes.STRING }
});

const Amende = sequelize.define('Amende', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    caisseId: { type: DataTypes.UUID, allowNull: false },
    memberId: { type: DataTypes.UUID, allowNull: false },
    cycleId: { type: DataTypes.UUID, allowNull: false },
    montant: { type: DataTypes.BIGINT, allowNull: false },
    motif: { type: DataTypes.STRING, allowNull: false },
    type: { type: DataTypes.STRING, defaultValue: 'AMENDE' }, // AMENDE, PENALITE
    statut: { type: DataTypes.STRING, defaultValue: 'A_PAYER' }, // A_PAYER, PAYE
    dateCreation: { type: DataTypes.BIGINT, defaultValue: Date.now() }
});

const SortieCaisse = sequelize.define('SortieCaisse', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    caisseId: { type: DataTypes.UUID, allowNull: false },
    montant: { type: DataTypes.BIGINT, allowNull: false },
    motif: { type: DataTypes.STRING },
    dateSortie: { type: DataTypes.BIGINT, defaultValue: Date.now() }
});

const Notification = sequelize.define('Notification', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    userId: { type: DataTypes.UUID, allowNull: false },
    title: { type: DataTypes.STRING, allowNull: false },
    message: { type: DataTypes.TEXT },
    type: { type: DataTypes.STRING }, // RECEIPT, ALERT, INVITE
    referenceId: { type: DataTypes.UUID }, // ID de la transaction ou tontine
    isRead: { type: DataTypes.BOOLEAN, defaultValue: false },
    createdAt: { type: DataTypes.BIGINT, defaultValue: Date.now() }
});

const InviteLink = sequelize.define('InviteLink', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    tontineId: { type: DataTypes.UUID, allowNull: false },
    senderId: { type: DataTypes.UUID, allowNull: false }, // L'utilisateur du bureau qui envoie
    code: { type: DataTypes.STRING, unique: true, allowNull: false },
    createdAt: { type: DataTypes.BIGINT, defaultValue: Date.now() }
});

const JoinRequest = sequelize.define('JoinRequest', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    tontineId: { type: DataTypes.UUID, allowNull: false },
    userId: { type: DataTypes.UUID, allowNull: false }, // Demandeur
    inviteCode: { type: DataTypes.STRING },
    status: { type: DataTypes.STRING, defaultValue: 'PENDING' }, // PENDING, ACCEPTED, REJECTED
    createdAt: { type: DataTypes.BIGINT, defaultValue: Date.now() }
});

const Comment = sequelize.define('Comment', {
    id: { type: DataTypes.UUID, defaultValue: DataTypes.UUIDV4, primaryKey: true },
    tontineId: { type: DataTypes.UUID, allowNull: false },
    userId: { type: DataTypes.UUID, allowNull: false },
    content: { type: DataTypes.TEXT, allowNull: false },
    imageUrl: { type: DataTypes.TEXT('long') }, // Stockage Base64 pour ce prototype
    createdAt: { type: DataTypes.BIGINT, defaultValue: Date.now() }
});

// --- RELATIONSHIPS ---
Tontine.hasMany(Member, { foreignKey: 'tontineId', as: 'members' });
Tontine.hasMany(Caisse, { foreignKey: 'tontineId', as: 'caisses' });
Tontine.hasMany(JoinRequest, { foreignKey: 'tontineId', as: 'requests' });
Tontine.hasMany(InviteLink, { foreignKey: 'tontineId', as: 'invites' });
Tontine.hasMany(Comment, { foreignKey: 'tontineId', as: 'comments' });

Member.belongsTo(Tontine, { foreignKey: 'tontineId' });
Member.belongsTo(User, { foreignKey: 'userId', as: 'userAccount' });

JoinRequest.belongsTo(User, { foreignKey: 'userId', as: 'requester' });
JoinRequest.belongsTo(Tontine, { foreignKey: 'tontineId' });

InviteLink.belongsTo(User, { foreignKey: 'senderId', as: 'sender' });

Comment.belongsTo(User, { foreignKey: 'userId', as: 'author' });
Comment.belongsTo(Tontine, { foreignKey: 'tontineId' });

Caisse.hasMany(Cycle, { foreignKey: 'caisseId', as: 'cycles' });
Cycle.belongsTo(Caisse, { foreignKey: 'caisseId' });

Caisse.belongsToMany(Member, { through: CaisseParticipant, as: 'participants', foreignKey: 'caisseId' });
Member.belongsToMany(Caisse, { through: CaisseParticipant, as: 'participations', foreignKey: 'memberId' });

Cycle.hasMany(Tour, { foreignKey: 'cycleId', as: 'tours' });
Tour.belongsTo(Cycle, { foreignKey: 'cycleId' });

Tour.belongsTo(Member, { foreignKey: 'beneficiaryId', as: 'beneficiary' });

Caisse.hasMany(Transaction, { foreignKey: 'caisseId', as: 'transactions' });
Transaction.belongsTo(Caisse, { foreignKey: 'caisseId' });

Caisse.hasMany(Pret, { foreignKey: 'caisseId', as: 'prets' });
Pret.belongsTo(Caisse, { foreignKey: 'caisseId' });

Member.hasMany(Transaction, { foreignKey: 'memberId', as: 'transactions' });
Transaction.belongsTo(Member, { foreignKey: 'memberId' });

Cycle.hasMany(Transaction, { foreignKey: 'cycleId', as: 'transactions' });
Transaction.belongsTo(Cycle, { foreignKey: 'cycleId' });

Transaction.belongsTo(User, { foreignKey: 'validatedById', as: 'validator' });

Transaction.hasOne(Pret, { foreignKey: 'transactionId', as: 'pret' });
Pret.belongsTo(Transaction, { foreignKey: 'transactionId' });

Member.hasMany(Pret, { foreignKey: 'memberId', as: 'prets' });
Pret.belongsTo(Member, { foreignKey: 'memberId' });

Caisse.hasMany(Amende, { foreignKey: 'caisseId', as: 'amendes' });
Amende.belongsTo(Caisse, { foreignKey: 'caisseId' });

Member.hasMany(Amende, { foreignKey: 'memberId', as: 'amendes' });
Amende.belongsTo(Member, { foreignKey: 'memberId' });

Caisse.hasMany(SortieCaisse, { foreignKey: 'caisseId', as: 'sorties' });
SortieCaisse.belongsTo(Caisse, { foreignKey: 'caisseId' });

// Sync Database
// Note: alter: true est réactivé pour s'assurer que les nouvelles colonnes (fraisCaisse, etc.) sont créées
sequelize.sync({ alter: true }).then(() => console.log("Database & tables synced and updated!"));

// --- MIDDLEWARE POUR VÉRIFIER L'APPARTENANCE ---
const checkMembership = async (req, res, next) => {
    const tontineId = req.params.tontineId || req.body.tontineId || req.query.tontineId;
    const caisseId = req.params.caisseId || req.body.caisseId || req.query.caisseId || req.params.id; // Supporte /caisses/:id
    const userId = req.body.userId || req.query.userId || req.body.adminUserId;

    if (!userId) return res.status(401).json({ error: "Identification requise." });

    try {
        let finalTontineId = tontineId;

        if (!finalTontineId && caisseId) {
            const caisse = await Caisse.findByPk(caisseId);
            if (caisse) finalTontineId = caisse.tontineId;
        }

        if (!finalTontineId) return next(); // Si pas de tontine/caisse concernée, on passe

        const tontine = await Tontine.findByPk(finalTontineId);
        if (!tontine) return next();

        // 1. Est-ce l'initiateur ?
        if (tontine.creatorId === userId) {
            console.log(`[NODE] Accès autorisé: User ${userId} est l'initiateur de Tontine ${finalTontineId}`);
            return next();
        }

        // 2. Est-ce un membre ?
        const member = await Member.findOne({ where: { tontineId: finalTontineId, userId } });
        if (!member) {
            console.log(`[NODE] Accès refusé: User ${userId} n'est ni initiateur ni membre de Tontine ${finalTontineId}`);
            return res.status(403).json({ error: "Accès refusé. Vous n'êtes pas membre de cette tontine." });
        }
        req.userMember = member; // Passer les infos du membre pour la suite si besoin
        next();
    } catch (e) {
        console.error(`[NODE] Erreur checkMembership: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
};

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
    console.log(`[NODE] POST /users/register - Body: ${JSON.stringify(req.body)}`);
    try {
        const { nom, phone, pin } = req.body;
        const user = await User.create({
            nom, phone, pinHash: pin, matricule: generateMatricule()
        });
        res.status(201).json(user);
    } catch (e) {
        console.error(`[NODE] Erreur register: ${e.message}`);
        res.status(400).json({ error: e.message });
    }
});

app.post('/users/login', async (req, res) => {
    console.log(`[NODE] POST /users/login - Body: ${JSON.stringify(req.body)}`);
    try {
        const { phone, pin } = req.body;
        const user = await User.findOne({ where: { phone, pinHash: pin } });
        if (!user) return res.status(401).json({ error: 'Identifiants invalides.' });
        res.json(user);
    } catch (e) {
        console.error(`[NODE] Erreur login: ${e.message}`);
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
    console.log(`[NODE] POST /tontines - Body: ${JSON.stringify(req.body)}`);
    try {
        const { id, libelle, exercice, dateDebut, dateFin, creatorId, isOnline } = req.body;

        if (id) {
            const existing = await Tontine.findByPk(id);
            if (existing) {
                console.log(`[NODE] Tontine ${id} déjà existante, retournée (Idempotence)`);
                return res.json(existing);
            }
        }

        const tontine = await Tontine.create({
            id: id || uuidv4(),
            libelle,
            exercice,
            dateDebut,
            dateFin,
            creatorId: creatorId,
            isOnline: isOnline !== undefined ? isOnline : true,
            updatedAt: Date.now()
        });

        if (creatorId) {
            console.log(`[NODE] Création tontine par ${creatorId}. Ajout optionnel comme membre initiateur.`);
            const user = await User.findByPk(creatorId);
            if (user) {
                // On l'ajoute quand même comme membre initiateur par défaut pour la visibilité,
                // mais le droit d'accès sera aussi basé sur creatorId.
                await Member.create({
                    tontineId: tontine.id,
                    userId: user.id,
                    nomSaisi: user.nom,
                    phone: user.phone,
                    role: 'INITIATEUR',
                    isInitiator: true,
                    joinedAt: Date.now()
                });
            }
        }
        console.log(`[NODE] Tontine créée avec succès: ${tontine.id}`);
        res.status(201).json(tontine);
    } catch (e) {
        console.error(`[NODE] Erreur creation tontine: ${e.message}`);
        res.status(400).json({ error: e.message });
    }
});

app.delete('/tontines/:id', async (req, res) => {
    console.log(`[NODE] Suppression tontine: ${req.params.id}`);
    try {
        const tontineId = req.params.id;

        // Supprimer les membres, caisses, etc. (Sequelize gérera via CASCADE si possible, sinon manuel)
        // Note: On fait le nettoyage manuel pour être sûr
        await Member.destroy({ where: { tontineId } });
        const caisses = await Caisse.findAll({ where: { tontineId } });
        for (const caisse of caisses) {
            await Cycle.destroy({ where: { caisseId: caisse.id } });
            await CaisseParticipant.destroy({ where: { caisseId: caisse.id } });
            await Transaction.destroy({ where: { caisseId: caisse.id } });
            await Caisse.destroy({ where: { id: caisse.id } });
        }

        const deleted = await Tontine.destroy({ where: { id: tontineId } });
        if (deleted) {
            res.json({ message: "Tontine supprimée avec succès." });
        } else {
            res.status(404).json({ error: "Tontine non trouvée" });
        }
    } catch (e) {
        console.error(`[NODE] Erreur suppression tontine: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.get('/tontines/search', async (req, res) => {
    console.log(`[NODE] GET /tontines/search - Query: ${req.query.query}, userId: ${req.query.userId}`);
    try {
        const { query, userId } = req.query;
        const results = await Tontine.findAll({
            where: {
                [Op.or]: [
                    { libelle: { [Op.like]: `%${query}%` } },
                    { id: query } // Permettre la recherche par ID direct aussi
                ]
            }
        });

        // Enrichir avec le statut d'adhésion si userId est fourni
        const enrichedResults = await Promise.all(results.map(async (t) => {
            let membershipStatus = 'NONE'; // NONE, PENDING, MEMBER

            if (userId) {
                if (t.creatorId === userId) {
                    membershipStatus = 'MEMBER'; // On le traite comme membre (accès total)
                } else {
                    const member = await Member.findOne({ where: { tontineId: t.id, userId } });
                    if (member) {
                        membershipStatus = 'MEMBER';
                    } else {
                        const request = await JoinRequest.findOne({ where: { tontineId: t.id, userId, status: 'PENDING' } });
                        if (request) membershipStatus = 'PENDING';
                    }
                }
            }

            return { ...t.toJSON(), membershipStatus };
        }));

        res.json(enrichedResults);
    } catch (e) {
        console.error(`[NODE] Erreur search tontines: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.post('/tontines/:id/join', async (req, res) => {
    console.log(`[NODE] POST /tontines/${req.params.id}/join - Body: ${JSON.stringify(req.body)}`);
    try {
        const { userId } = req.body;
        const tontineId = req.params.id;

        // Vérifier si déjà membre
        const alreadyMember = await Member.findOne({ where: { tontineId, userId } });
        if (alreadyMember) return res.status(400).json({ error: "Vous êtes déjà membre de cette tontine." });

        // Vérifier si demande déjà en cours
        const existingRequest = await JoinRequest.findOne({ where: { tontineId, userId, status: 'PENDING' } });
        if (existingRequest) return res.status(400).json({ error: "Une demande d'adhésion est déjà en cours." });

        const request = await JoinRequest.create({
            tontineId,
            userId,
            status: 'PENDING',
            createdAt: Date.now()
        });

        console.log(`[NODE] Demande d'adhésion créée pour user ${userId} dans tontine ${tontineId}`);
        res.status(201).json(request);
    } catch (e) {
        console.error(`[NODE] Erreur join request: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.get('/tontines/:id', async (req, res) => {
    console.log(`[NODE] GET /tontines/${req.params.id} - userId: ${req.query.userId}`);
    try {
        const { userId } = req.query;
        const tontineId = req.params.id;

        if (!userId) return res.status(401).json({ error: "Identification requise." });

        const tontine = await Tontine.findByPk(tontineId, {
            include: [
                { model: Member, as: 'members' },
                {
                    model: Caisse,
                    as: 'caisses',
                    include: [{ model: Transaction, as: 'transactions' }]
                }
            ]
        });
        if (!tontine) {
            console.log(`[NODE] Tontine non trouvée: ${tontineId}`);
            return res.status(404).json({ error: 'Tontine non trouvée' });
        }

        // VÉRIFICATION D'ACCÈS : Initiateur OU Membre
        const member = await Member.findOne({ where: { tontineId, userId } });
        const isInitiator = tontine.creatorId === userId;

        if (!member && !isInitiator) {
            console.log(`[NODE] Accès refusé à la tontine ${tontineId} pour user ${userId}`);
            return res.status(403).json({ error: "Accès refusé. Vous devez être membre ou initiateur pour accéder à cette tontine." });
        }

        // Calcul du résumé financier (Simplifié pour le dev)
        const globalBalance = await Transaction.sum('montant', { where: { '$Caisse.tontineId$': tontineId }, include: [{ model: Caisse }] }) || 0;
        const totalActiveLoans = await Pret.sum('montantRestant', { where: { statut: 'ACTIF' }, include: [{ model: Member, where: { tontineId } }] }) || 0;

        const summary = {
            globalBalance,
            turnoverIn: 0,
            turnoverOut: 0,
            totalActiveLoans,
            forecastGains: Math.floor(totalActiveLoans * 0.1),
            lendableAmount: Math.max(0, globalBalance - 50000),
            nonLendableAmount: 50000
        };

        res.json({ ...tontine.toJSON(), summary });
        console.log(`[NODE] Détails envoyés pour tontine ${tontineId}. Membres: ${tontine.members.length}`);
    } catch (e) {
        console.error(`[NODE] Erreur details tontine: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.get('/users/:id/tontines', async (req, res) => {
    console.log(`[NODE] GET /users/${req.params.id}/tontines`);
    try {
        const { id } = req.params;
        const results = await Tontine.findAll({
            include: [
                {
                    model: Member,
                    as: 'members',
                    required: false // On ne force pas l'appartenance via Member uniquement
                },
                {
                    model: Caisse,
                    as: 'caisses'
                }
            ],
            where: {
                [Op.or]: [
                    { creatorId: id },
                    { '$members.userId$': id }
                ]
            }
        });

        // Ajouter le résumé financier pour chaque tontine
        const tontinesWithSummary = await Promise.all(results.map(async (t) => {
            const globalBalance = await Transaction.sum('montant', {
                where: { '$Caisse.tontineId$': t.id },
                include: [{ model: Caisse }]
            }) || 0;
            const totalActiveLoans = await Pret.sum('montantRestant', {
                where: { statut: 'ACTIF' },
                include: [{ model: Member, where: { tontineId: t.id } }]
            }) || 0;

            const summary = {
                globalBalance,
                turnoverIn: 0,
                turnoverOut: 0,
                totalActiveLoans,
                forecastGains: 0,
                lendableAmount: Math.max(0, globalBalance - 50000),
                nonLendableAmount: 50000
            };
            return { ...t.toJSON(), summary };
        }));

        res.json(tontinesWithSummary);
    } catch (e) {
        console.error(`[NODE] Erreur my tontines: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.get('/users/:id/signature-stats', async (req, res) => {
    console.log(`[NODE] GET /users/${req.params.id}/signature-stats`);
    try {
        const { id } = req.params;
        const now = new Date();
        const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1).getTime();

        const count = await Transaction.count({
            where: {
                validatedById: id,
                dateTransaction: { [Op.gte]: startOfMonth }
            }
        });

        const totalAmount = await Transaction.sum('montant', {
            where: {
                validatedById: id,
                dateTransaction: { [Op.gte]: startOfMonth },
                montant: { [Op.gt]: 0 }
            }
        }) || 0;

        res.json({
            count,
            totalAmount,
            month: now.toLocaleString('default', { month: 'long' })
        });
    } catch (e) {
        console.error(`[NODE] Erreur signature stats: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.get('/tontines/:id/stats', async (req, res) => {
    try {
        const { id } = req.params;

        // 1. Évolution de la balance sur les 6 derniers mois
        // Pour ce proto, on va simuler des données basées sur les transactions réelles
        const transactions = await Transaction.findAll({
            include: [{ model: Caisse, where: { tontineId: id } }],
            order: [['dateTransaction', 'ASC']]
        });

        const monthlyStats = {};
        let runningBalance = 0;

        transactions.forEach(t => {
            const date = new Date(parseInt(t.dateTransaction));
            const month = date.toLocaleString('default', { month: 'short' });
            runningBalance += parseInt(t.montant);
            monthlyStats[month] = runningBalance;
        });

        // 2. Répartition par type de revenus (Cotisations vs Amendes vs Remboursements)
        const incomeByType = await Transaction.findAll({
            attributes: ['type', [sequelize.fn('SUM', sequelize.col('montant')), 'total']],
            include: [{ model: Caisse, where: { tontineId: id } }],
            where: { montant: { [Op.gt]: 0 } },
            group: ['type']
        });

        res.json({
            growth: monthlyStats,
            distribution: incomeByType
        });
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

// --- INVITATIONS & MAPPING ---

// Générer un lien d'invitation
app.post('/tontines/:id/invites', async (req, res) => {
    console.log(`[NODE] POST /tontines/${req.params.id}/invites - Body: ${JSON.stringify(req.body)}`);
    try {
        const { id } = req.params;
        const { senderId } = req.body;

        // Vérifier si l'expéditeur a le droit (Bureau)
        const member = await Member.findOne({ where: { tontineId: id, userId: senderId } });
        if (!member || member.role === 'MEMBRE') {
            return res.status(403).json({ error: "Seuls les membres du bureau peuvent inviter." });
        }

        const code = Math.random().toString(36).substring(2, 8).toUpperCase();
        const invite = await InviteLink.create({
            tontineId: id,
            senderId,
            code
        });
        console.log(`[NODE] Invitation générée: ${code}`);
        res.status(201).json(invite);
    } catch (e) {
        console.error(`[NODE] Erreur generation invite: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.get('/invites/:code', async (req, res) => {
    console.log(`[NODE] GET /invites/${req.params.code}`);
    try {
        const invite = await InviteLink.findOne({
            where: { code: req.params.code },
            include: [
                { model: Tontine },
                { model: User, as: 'sender', attributes: ['nom'] }
            ]
        });
        if (!invite) return res.status(404).json({ error: "Lien invalide ou expiré." });
        res.json(invite);
    } catch (e) {
        console.error(`[NODE] Erreur details invite: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.post('/invites/:code/join', async (req, res) => {
    console.log(`[NODE] POST /invites/${req.params.code}/join - Body: ${JSON.stringify(req.body)}`);
    try {
        const { userId } = req.body;
        const invite = await InviteLink.findOne({ where: { code: req.params.code } });
        if (!invite) return res.status(404).json({ error: "Lien invalide." });

        // Vérifier si déjà membre
        const alreadyMember = await Member.findOne({ where: { tontineId: invite.tontineId, userId } });
        if (alreadyMember) return res.status(400).json({ error: "Vous êtes déjà membre de cette tontine." });

        // Créer la demande
        const request = await JoinRequest.create({
            tontineId: invite.tontineId,
            userId,
            inviteCode: req.params.code
        });
        console.log(`[NODE] Demande d'adhésion créée: ${request.id}`);
        res.status(201).json(request);
    } catch (e) {
        console.error(`[NODE] Erreur join: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

// Lister les demandes d'une tontine
app.get('/tontines/:id/requests', async (req, res) => {
    console.log(`[NODE] GET /tontines/${req.params.id}/requests`);
    try {
        const requests = await JoinRequest.findAll({
            where: { tontineId: req.params.id, status: 'PENDING' },
            include: [
                { model: User, as: 'requester', attributes: ['id', 'nom', 'phone', 'matricule'] }
            ]
        });
        console.log(`[NODE] ${requests.length} demandes en attente pour tontine ${req.params.id}`);
        res.json(requests);
    } catch (e) {
        console.error(`[NODE] Erreur get requests: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.post('/requests/:id/approve', async (req, res) => {
    console.log(`[NODE] POST /requests/${req.params.id}/approve - Body: ${JSON.stringify(req.body)}`);
    try {
        const { id } = req.params;
        const { adminUserId, targetMemberId, updateInfo } = req.body;

        const request = await JoinRequest.findByPk(id, { include: [{ model: User, as: 'requester' }] });
        if (!request) return res.status(404).json({ error: "Demande introuvable." });

        const adminMember = await Member.findOne({ where: { tontineId: request.tontineId, userId: adminUserId } });
        if (!adminMember) return res.status(403).json({ error: "Action non autorisée. Vous n'êtes pas membre de cette tontine." });

        // LOGIQUE HIÉRARCHIQUE ET TEMPORELLE
        const now = Date.now();
        const requestAgeHours = (now - request.createdAt) / (1000 * 60 * 60);
        const role = adminMember.role;

        let canApprove = false;
        let reason = "";

        if (role === 'PRESIDENT' || adminMember.isInitiator) {
            canApprove = true;
        } else if (role === 'VICE_PRESIDENT') {
            if (requestAgeHours >= 24) {
                canApprove = true;
            } else {
                reason = "Le Vice-Président doit attendre 24h avant de pouvoir valider si le Président n'a pas agi.";
            }
        } else if (['SECRETAIRE', 'TRESORIER'].includes(role)) {
            if (requestAgeHours >= 48) {
                canApprove = true;
            } else {
                reason = "Le Secrétaire/Trésorier doit attendre 48h avant de pouvoir valider.";
            }
        } else {
            reason = "Votre rôle ne vous permet pas de valider des adhésions.";
        }

        if (!canApprove) {
            return res.status(403).json({ error: reason });
        }

        // MAPPAGE
        const member = await Member.findByPk(targetMemberId);
        if (!member) return res.status(404).json({ error: "Membre cible introuvable." });

        const updateData = { userId: request.userId };
        if (updateInfo) {
            updateData.nomSaisi = request.requester.nom;
            updateData.phone = request.requester.phone;
        }

        await member.update(updateData);
        await request.update({ status: 'ACCEPTED' });
        console.log(`[NODE] Demande approuvée et membre mappé: ${member.id}`);
        res.json({ message: "Membre mappé avec succès !", member });
    } catch (e) {
        console.error(`[NODE] Erreur approve request: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

// --- CAISSE ENDPOINTS ---

app.post('/caisses', async (req, res) => {
    console.log(`[NODE] POST /caisses - Body: ${JSON.stringify(req.body)}`);
    try {
        const {
            id, tontineId, type, libelle, montantFixe, montantMinimum, regulariteImposee,
            redistribution, possibilitePret, autoriserPretSurAmendes,
            tauxInteret, typeInteret, penaliteRetardFixe, penaliteEchecFixe,
            dateLimiteJour, fraisCaisse, respecterFrequenceStrictement,
            frequence, dureeCycles, plafondPret, isBasique
        } = req.body;

        if (id) {
            const existing = await Caisse.findByPk(id);
            if (existing) {
                console.log(`[NODE] Caisse ${id} déjà existante, retournée (Idempotence)`);
                return res.json(existing);
            }
        }

        const caisse = await Caisse.create({
            id: id || uuidv4(),
            tontineId,
            type,
            libelle,
            montantFixe,
            montantMinimum,
            regulariteImposee,
            redistribution,
            possibilitePret,
            autoriserPretSurAmendes,
            tauxInteret,
            typeInteret,
            penaliteRetardFixe,
            penaliteEchecFixe,
            dateLimiteJour,
            fraisCaisse,
            respecterFrequenceStrictement,
            frequence: frequence || 'MENSUEL',
            dureeCycles,
            plafondPret,
            isBasique,
            updatedAt: Date.now()
        });

        // Créer automatiquement le Cycle #1
        await Cycle.create({
            caisseId: caisse.id,
            numeroCycle: 1,
            dateDebut: Date.now(),
            estCloture: false
        });
        console.log(`[NODE] Caisse créée avec succès: ${caisse.id}`);
        res.status(201).json(caisse);
    } catch (e) {
        console.error(`[NODE] Erreur creation caisse: ${e.message}`);
        res.status(400).json({ error: e.message });
    }
});

app.post('/tontines/:id/members', async (req, res) => {
    console.log(`[NODE] POST /tontines/${req.params.id}/members - Body: ${JSON.stringify(req.body)}`);
    try {
        const tontineId = req.params.id;
        const { nomSaisi, phone, role } = req.body;

        const member = await Member.create({
            id: uuidv4(),
            tontineId,
            nomSaisi,
            phone,
            role: role || 'MEMBRE',
            joinedAt: Date.now()
        });
        console.log(`[NODE] Membre ajouté avec succès: ${member.id}`);
        res.status(201).json(member);
    } catch (e) {
        console.error(`[NODE] Erreur ajout membre: ${e.message}`);
        res.status(400).json({ error: e.message });
    }
});

app.delete('/caisses/:id', async (req, res) => {
    console.log(`[NODE] Suppression caisse: ${req.params.id}`);
    try {
        const caisseId = req.params.id;

        // 1. Vérifier s'il y a des transactions
        const txCount = await Transaction.count({ where: { caisseId } });
        if (txCount > 0) {
            return res.status(400).json({ error: "Impossible de supprimer une caisse qui possède déjà des transactions (cotisations, prêts, etc.)." });
        }

        // 2. Supprimer la caisse (Sequelize gérera les cycles associés via CASCADE si configuré, sinon on le fait manuellement)
        await Cycle.destroy({ where: { caisseId } });
        await CaisseParticipant.destroy({ where: { caisseId } });
        await Caisse.destroy({ where: { id: caisseId } });

        res.json({ message: "Caisse supprimée avec succès." });
    } catch (e) {
        console.error(`[NODE] Erreur suppression caisse: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.get('/caisses/:id', async (req, res) => {
    try {
        const { userId } = req.query;
        const caisseId = req.params.id;

        if (!userId) return res.status(401).json({ error: "Identification requise." });

        const caisse = await Caisse.findByPk(caisseId, {
            include: [
                { model: Cycle, as: 'cycles', where: { estCloture: false }, limit: 1 },
                { model: Transaction, as: 'transactions' },
                { model: Tontine, as: 'tontine' }
            ]
        });

        if (!caisse) return res.status(404).json({ error: 'Caisse non trouvée' });

        // VÉRIFICATION D'ACCÈS : Initiateur OU Membre
        const member = await Member.findOne({ where: { tontineId: caisse.tontineId, userId } });
        const isInitiator = caisse.tontine?.creatorId === userId;

        if (!member && !isInitiator) {
            console.log(`[NODE] Accès refusé à la caisse ${caisseId} pour user ${userId}`);
            return res.status(403).json({ error: "Accès refusé. Vous n'êtes pas membre de cette tontine." });
        }

        res.json(caisse);
    } catch (e) {
        console.error(`[NODE] Erreur details caisse: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.post('/transactions', async (req, res) => {
    console.log(`[NODE] POST /transactions - Body: ${JSON.stringify(req.body)}`);
    try {
        const { caisseId, memberId, cycleId, montant, type, motif, validatedById } = req.body;

        const transaction = await Transaction.create({
            caisseId,
            memberId,
            cycleId,
            validatedById,
            montant,
            type,
            motif,
            dateTransaction: Date.now()
        });

        // Envoyer une notification au membre si c'est un encaissement (positif)
        if (montant > 0) {
            const member = await Member.findByPk(memberId);
            const caisse = await Caisse.findByPk(caisseId);
            if (member && member.userId) {
                await Notification.create({
                    userId: member.userId,
                    title: "Nouveau Reçu de Paiement",
                    message: `Votre versement de ${montant} F pour la caisse ${caisse.libelle} a été validé.`,
                    type: 'RECEIPT',
                    referenceId: transaction.id
                });
            }
        }
        console.log(`[NODE] Transaction créée avec succès: ${transaction.id}`);
        res.status(201).json(transaction);
    } catch (e) {
        console.error(`[NODE] Erreur creation transaction: ${e.message}`);
        res.status(400).json({ error: e.message });
    }
});

// --- LOAN (PRET) ENDPOINTS ---

app.post('/prets', async (req, res) => {
    console.log(`[NODE] POST /prets - Body: ${JSON.stringify(req.body)}`);
    try {
        const { caisseId, memberId, montantPrincipal, montantInterets, tauxInteret, typeInteret, dateEcheance, nomEmprunteurExterne, cniEmprunteurExterne } = req.body;

        const pret = await Pret.create({
            caisseId,
            memberId,
            montantPrincipal,
            montantInterets,
            montantRestant: parseInt(montantPrincipal) + parseInt(montantInterets || 0),
            tauxInteret,
            typeInteret,
            dateEcheance,
            nomEmprunteurExterne,
            cniEmprunteurExterne,
            statut: 'PENDING'
        });
        console.log(`[NODE] Prêt créé avec succès: ${pret.id}`);
        res.status(201).json(pret);
    } catch (e) {
        console.error(`[NODE] Erreur creation pret: ${e.message}`);
        res.status(400).json({ error: e.message });
    }
});

app.post('/prets/:id/approve', async (req, res) => {
    console.log(`[NODE] POST /prets/${req.params.id}/approve`);
    try {
        const pret = await Pret.findByPk(req.params.id, { include: [{ model: Caisse }] });
        if (!pret) return res.status(404).json({ error: "Prêt introuvable" });

        const cycle = await Cycle.findOne({ where: { caisseId: pret.caisseId, estCloture: false } });
        if (!cycle) return res.status(400).json({ error: "Aucun cycle actif pour cette caisse." });

        await Transaction.create({
            caisseId: pret.caisseId,
            memberId: pret.memberId || '00000000-0000-0000-0000-000000000000',
            cycleId: cycle.id,
            montant: -pret.montantPrincipal,
            type: 'PRET',
            motif: `Prêt accordé à ${pret.nomEmprunteurExterne || 'un membre'}`
        });

        await pret.update({ statut: 'ACTIF' });
        console.log(`[NODE] Prêt approuvé et décaissé: ${pret.id}`);
        res.json({ message: "Prêt validé et décaissé !", pret });
    } catch (e) {
        console.error(`[NODE] Erreur approbation pret: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.post('/prets/:id/repay', async (req, res) => {
    console.log(`[NODE] POST /prets/${req.params.id}/repay - Body: ${JSON.stringify(req.body)}`);
    try {
        const { id } = req.params;
        const { montant, memberId, motif, validatedById } = req.body;

        const pret = await Pret.findByPk(id);
        if (!pret) return res.status(404).json({ error: "Prêt introuvable" });
        if (pret.statut !== 'ACTIF') return res.status(400).json({ error: "Ce prêt n'est pas actif." });

        const cycle = await Cycle.findOne({ where: { caisseId: pret.caisseId, estCloture: false } });
        if (!cycle) return res.status(400).json({ error: "Aucun cycle actif pour cette caisse." });

        // Créer la transaction d'entrée d'argent
        const transaction = await Transaction.create({
            caisseId: pret.caisseId,
            memberId: memberId || pret.memberId || '00000000-0000-0000-0000-000000000000',
            cycleId: cycle.id,
            validatedById: validatedById,
            montant: parseInt(montant),
            type: 'REMBOURSEMENT',
            motif: motif || `Remboursement de prêt`
        });

        // Mettre à jour le montant restant
        const nouveauReste = Math.max(0, pret.montantRestant - parseInt(montant));
        const nouveauStatut = nouveauReste === 0 ? 'SOLDE' : 'ACTIF';

        await pret.update({
            montantRestant: nouveauReste,
            statut: nouveauStatut
        });

        // Notification de remboursement
        const member = await Member.findByPk(memberId || pret.memberId);
        if (member && member.userId) {
            await Notification.create({
                userId: member.userId,
                title: "Remboursement Validé",
                message: `Votre remboursement de ${montant} F a été bien enregistré. Reste à payer: ${nouveauReste} F.`,
                type: 'RECEIPT',
                referenceId: transaction.id
            });
        }
        console.log(`[NODE] Remboursement enregistré pour le prêt: ${pret.id}`);
        res.json({ message: "Remboursement enregistré !", pret });
    } catch (e) {
        console.error(`[NODE] Erreur remboursement pret: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.post('/tontines/:id/broadcast-report', async (req, res) => {
    console.log(`[NODE] POST /tontines/${req.params.id}/broadcast-report - Body: ${JSON.stringify(req.body)}`);
    try {
        const { id } = req.params;
        const { senderId, reportData } = req.body;

        const tontine = await Tontine.findByPk(id, {
            include: [{ model: Member, as: 'members' }]
        });

        if (!tontine) return res.status(404).json({ error: "Tontine non trouvée" });

        // Créer une notification pour chaque membre ayant un compte lié
        const notifications = tontine.members
            .filter(m => m.userId && m.userId !== senderId)
            .map(m => ({
                userId: m.userId,
                title: `Rapport Financier : ${tontine.libelle}`,
                message: `Le rapport financier de la période vient d'être publié par le bureau. Consultez-le dans votre profil.`,
                type: 'REPORT',
                referenceId: tontine.id
            }));

        await Notification.bulkCreate(notifications);
        console.log(`[NODE] Rapport diffusé à ${notifications.length} membres.`);
        res.json({ message: `Rapport diffusé à ${notifications.length} membres.` });
    } catch (e) {
        console.error(`[NODE] Erreur broadcast report: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

// --- COMMENT ENDPOINTS ---

app.post('/tontines/:id/comments', async (req, res) => {
    console.log(`[NODE] POST /tontines/${req.params.id}/comments - Body: ${JSON.stringify(req.body)}`);
    try {
        const { id } = req.params;
        const { userId, content, imageUrl } = req.body;

        if (!content && !imageUrl) {
            return res.status(400).json({ error: "Le commentaire ou l'image ne peut pas être vide." });
        }

        const comment = await Comment.create({
            tontineId: id,
            userId,
            content: content || "",
            imageUrl,
            createdAt: Date.now()
        });

        const commentWithAuthor = await Comment.findByPk(comment.id, {
            include: [{ model: User, as: 'author', attributes: ['nom'] }]
        });
        console.log(`[NODE] Commentaire créé: ${comment.id}`);
        res.status(201).json(commentWithAuthor);
    } catch (e) {
        console.error(`[NODE] Erreur creation commentaire: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.get('/tontines/:id/comments', async (req, res) => {
    console.log(`[NODE] GET /tontines/${req.params.id}/comments`);
    try {
        const { id } = req.params;
        const { userId } = req.query;

        if (!userId) return res.status(401).json({ error: "Identification requise." });
        const member = await Member.findOne({ where: { tontineId: id, userId } });
        if (!member) return res.status(403).json({ error: "Accès refusé." });

        const comments = await Comment.findAll({
            where: { tontineId: id },
            include: [{ model: User, as: 'author', attributes: ['nom'] }],
            order: [['createdAt', 'ASC']]
        });
        res.json(comments);
    } catch (e) {
        console.error(`[NODE] Erreur get commentaires: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

// --- AMENDE ENDPOINTS ---

app.post('/amendes', async (req, res) => {
    console.log(`[NODE] POST /amendes - Body: ${JSON.stringify(req.body)}`);
    try {
        const { caisseId, memberId, cycleId, montant, motif, type } = req.body;
        const amende = await Amende.create({
            caisseId, memberId, cycleId, montant, motif, type, statut: 'A_PAYER'
        });
        console.log(`[NODE] Amende créée avec succès: ${amende.id}`);
        res.status(201).json(amende);
    } catch (e) {
        console.error(`[NODE] Erreur creation amende: ${e.message}`);
        res.status(400).json({ error: e.message });
    }
});

app.post('/amendes/:id/pay', async (req, res) => {
    console.log(`[NODE] POST /amendes/${req.params.id}/pay - Body: ${JSON.stringify(req.body)}`);
    try {
        const { validatedById } = req.body;
        const amende = await Amende.findByPk(req.params.id);
        if (!amende) return res.status(404).json({ error: "Amende introuvable" });

        // Créer la transaction d'entrée
        await Transaction.create({
            caisseId: amende.caisseId,
            memberId: amende.memberId,
            cycleId: amende.cycleId,
            validatedById: validatedById,
            montant: amende.montant,
            type: amende.type === 'PENALITE' ? 'PENALITE' : 'AMENDE',
            motif: `Paiement ${amende.type}: ${amende.motif}`
        });

        await amende.update({ statut: 'PAYE' });
        console.log(`[NODE] Amende payée: ${amende.id}`);
        res.json({ message: "Paiement enregistré !", amende });
    } catch (e) {
        console.error(`[NODE] Erreur paiement amende: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.get('/tontines/:id/amendes', async (req, res) => {
    try {
        const amendes = await Amende.findAll({
            where: { statut: 'A_PAYER' },
            include: [{ model: Caisse, where: { tontineId: req.params.id } }]
        });
        res.json(amendes);
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

// --- EXPENDITURE (SORTIE) ENDPOINTS ---

app.post('/sorties', async (req, res) => {
    console.log(`[NODE] POST /sorties - Body: ${JSON.stringify(req.body)}`);
    try {
        const { caisseId, montant, motif } = req.body;

        const cycle = await Cycle.findOne({ where: { caisseId, estCloture: false } });
        if (!cycle) return res.status(400).json({ error: "Aucun cycle actif pour cette caisse." });

        const sortie = await SortieCaisse.create({
            caisseId,
            montant: parseInt(montant),
            motif,
            dateSortie: Date.now()
        });

        // Créer la transaction de sortie associée
        await Transaction.create({
            caisseId,
            memberId: '00000000-0000-0000-0000-000000000000', // ID Système
            cycleId: cycle.id,
            montant: -parseInt(montant),
            type: 'SORTIE',
            motif: `Dépense: ${motif}`
        });
        console.log(`[NODE] Sortie créée avec succès: ${sortie.id}`);
        res.status(201).json(sortie);
    } catch (e) {
        console.error(`[NODE] Erreur creation sortie: ${e.message}`);
        res.status(400).json({ error: e.message });
    }
});

// --- CYCLE ENDPOINTS ---

app.post('/caisses/:id/close-cycle', async (req, res) => {
    console.log(`[NODE] POST /caisses/${req.params.id}/close-cycle`);
    try {
        const caisseId = req.params.id;

        // 1. Trouver le cycle actuel
        const currentCycle = await Cycle.findOne({
            where: { caisseId, estCloture: false }
        });

        if (!currentCycle) {
            return res.status(400).json({ error: "Aucun cycle actif à clôturer." });
        }

        // 2. Clôturer le cycle actuel
        await currentCycle.update({
            estCloture: true,
            dateFin: Date.now()
        });

        // 3. Créer le nouveau cycle
        const nextCycle = await Cycle.create({
            caisseId,
            numeroCycle: currentCycle.numeroCycle + 1,
            dateDebut: Date.now(),
            estCloture: false
        });
        console.log(`[NODE] Cycle clôturé et nouveau cycle créé: ${nextCycle.id}`);
        res.json({
            message: `Cycle #${currentCycle.numeroCycle} clôturé. Bienvenue au Cycle #${nextCycle.numeroCycle} !`,
            newCycle: nextCycle
        });
    } catch (e) {
        console.error(`[NODE] Erreur clôture cycle: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

// --- TOURS & PARTICIPANTS ENDPOINTS ---

app.get('/caisses/:id/participants', async (req, res) => {
    console.log(`[NODE] GET /caisses/${req.params.id}/participants - userId: ${req.query.userId}`);
    try {
        const { userId } = req.query;
        const caisseId = req.params.id;

        const caisse = await Caisse.findByPk(caisseId);
        if (!caisse) {
            console.log(`[NODE] Caisse non trouvée: ${caisseId}`);
            return res.status(404).json({ error: 'Caisse non trouvée' });
        }

        if (userId) {
            const member = await Member.findOne({ where: { tontineId: caisse.tontineId, userId } });
            const tontine = await Tontine.findByPk(caisse.tontineId);
            const isInitiator = tontine && tontine.creatorId === userId;

            if (!member && !isInitiator) {
                console.log(`[NODE] Accès refusé aux participants: User ${userId} n'est ni membre ni initiateur`);
                return res.status(403).json({ error: "Accès refusé." });
            }
        } else {
            console.log(`[NODE] Accès refusé: Aucun userId fourni`);
            return res.status(401).json({ error: "Identification requise." });
        }

        const participants = await CaisseParticipant.findAll({
            where: { caisseId: req.params.id },
            include: [{ model: Member, as: 'member' }],
            order: [['ordrePassage', 'ASC']]
        });
        console.log(`[NODE] Retour de ${participants.length} participants pour caisse ${caisseId}`);
        res.json(participants);
    } catch (e) {
        console.error(`[NODE] Erreur get participants: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.post('/caisses/:id/participants', async (req, res) => {
    console.log(`[NODE] POST /caisses/${req.params.id}/participants - Body: ${JSON.stringify(req.body)}`);
    try {
        const { memberId, ordrePassage, nombreParts } = req.body;
        const part = await CaisseParticipant.create({
            caisseId: req.params.id,
            memberId,
            ordrePassage: ordrePassage || 0,
            nombreParts: nombreParts || 1
        });
        console.log(`[NODE] Participant ajouté à la caisse: ${memberId}`);
        res.status(201).json(part);
    } catch (e) {
        console.error(`[NODE] Erreur add participant: ${e.message}`);
        res.status(400).json({ error: e.message });
    }
});

app.put('/caisses/:caisseId/participants/:memberId/order', async (req, res) => {
    console.log(`[NODE] PUT /caisses/${req.params.caisseId}/participants/${req.params.memberId}/order - Body: ${JSON.stringify(req.body)}`);
    try {
        const { caisseId, memberId } = req.params;
        const { ordrePassage } = req.body;
        const part = await CaisseParticipant.findOne({ where: { caisseId, memberId } });
        if (!part) return res.status(404).json({ error: "Participant non trouvé" });
        await part.update({ ordrePassage });
        console.log(`[NODE] Ordre du participant mis à jour: ${memberId}`);
        res.json(part);
    } catch (e) {
        console.error(`[NODE] Erreur update order: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.get('/caisses/:id/tours/current', async (req, res) => {
    console.log(`[NODE] GET /caisses/${req.params.id}/tours/current`);
    try {
        const { userId } = req.query;
        const caisseId = req.params.id;

        const caisse = await Caisse.findByPk(caisseId);
        if (!caisse) return res.status(404).json({ error: 'Caisse non trouvée' });

        if (userId) {
            const member = await Member.findOne({ where: { tontineId: caisse.tontineId, userId } });
            if (!member) return res.status(403).json({ error: "Accès refusé." });
        } else {
            return res.status(401).json({ error: "Identification requise." });
        }

        const cycle = await Cycle.findOne({ where: { caisseId: req.params.id, estCloture: false } });
        if (!cycle) return res.status(404).json({ error: "Aucun cycle actif" });

        const tour = await Tour.findOne({
            where: { cycleId: cycle.id, estTermine: false },
            include: [{ model: Member, as: 'beneficiary' }],
            order: [['numeroTour', 'ASC']]
        });
        res.json(tour);
    } catch (e) {
        console.error(`[NODE] Erreur get current tour: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.post('/caisses/:id/tours', async (req, res) => {
    console.log(`[NODE] POST /caisses/${req.params.id}/tours - Body: ${JSON.stringify(req.body)}`);
    try {
        const cycle = await Cycle.findOne({ where: { caisseId: req.params.id, estCloture: false } });
        if (!cycle) return res.status(400).json({ error: "Aucun cycle actif" });

        const { beneficiaryId, numeroTour, dateTour } = req.body;
        const tour = await Tour.create({
            cycleId: cycle.id,
            beneficiaryId,
            numeroTour,
            dateTour: dateTour || Date.now(),
            estTermine: false
        });
        console.log(`[NODE] Tour créé: ${tour.id}`);
        res.status(201).json(tour);
    } catch (e) {
        console.error(`[NODE] Erreur creation tour: ${e.message}`);
        res.status(400).json({ error: e.message });
    }
});

app.post('/caisses/:id/distribute', async (req, res) => {
    console.log(`[NODE] POST /caisses/${req.params.id}/distribute - Body: ${JSON.stringify(req.body)}`);
    try {
        const caisseId = req.params.id;
        const { montant, beneficiaryId, adminUserId } = req.body;

        const cycle = await Cycle.findOne({ where: { caisseId, estCloture: false } });
        if (!cycle) return res.status(400).json({ error: "Aucun cycle actif pour cette caisse." });

        const tour = await Tour.findOne({ where: { cycleId: cycle.id, estTermine: false, beneficiaryId } });
        if (!tour) return res.status(400).json({ error: "Aucun tour en cours pour ce bénéficiaire." });

        // Créer la transaction de distribution (sortie d'argent)
        const transaction = await Transaction.create({
            caisseId,
            memberId: beneficiaryId,
            cycleId: cycle.id,
            validatedById: adminUserId,
            montant: -parseInt(montant),
            type: 'TOUR_ENCAISSER',
            motif: `Remise de la cagnotte au bénéficiaire`,
            dateTransaction: Date.now()
        });

        // Marquer le tour comme terminé
        await tour.update({ estTermine: true });

        // Notification au bénéficiaire
        const beneficiary = await Member.findByPk(beneficiaryId);
        if (beneficiary && beneficiary.userId) {
            await Notification.create({
                userId: beneficiary.userId,
                title: "Cagnotte Reçue !",
                message: `Félicitations ! Vous venez de recevoir la cagnotte de ${montant} F pour la caisse ${cycle.caisseId}.`,
                type: 'RECEIPT',
                referenceId: transaction.id
            });
        }
        console.log(`[NODE] Cagnotte distribuée: ${tour.id}`);
        res.json({ message: "Cagnotte distribuée avec succès !", tour });
    } catch (e) {
        console.error(`[NODE] Erreur distribution cagnotte: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.get('/users/:id/notifications', async (req, res) => {
    console.log(`[NODE] GET /users/${req.params.id}/notifications`);
    try {
        const notifications = await Notification.findAll({
            where: { userId: req.params.id },
            order: [['createdAt', 'DESC']]
        });
        res.json(notifications);
    } catch (e) {
        console.error(`[NODE] Erreur get notifications: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.post('/notifications/:id/read', async (req, res) => {
    console.log(`[NODE] POST /notifications/${req.params.id}/read`);
    try {
        const notification = await Notification.findByPk(req.params.id);
        if (notification) await notification.update({ isRead: true });
        res.json({ success: true });
    } catch (e) {
        console.error(`[NODE] Erreur read notification: ${e.message}`);
        res.status(500).json({ error: e.message });
    }
});

app.get('/transactions/:id', async (req, res) => {
    console.log(`[NODE] GET /transactions/${req.params.id}`);
    try {
        const transaction = await Transaction.findByPk(req.params.id, {
            include: [
                { model: Caisse },
                { model: User, as: 'validator', attributes: ['nom'] }
            ]
        });
        res.json(transaction);
    } catch (e) {
        console.error(`[NODE] Erreur get transaction: ${e.message}`);
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
app.get('/caisse-details', (req, res) => res.sendFile(path.join(__dirname, 'public', 'caisse_details.html')));
app.get('/receipt', (req, res) => res.sendFile(path.join(__dirname, 'public', 'receipt.html')));
app.get('/loans', (req, res) => res.sendFile(path.join(__dirname, 'public', 'loan_management.html')));
app.get('/record-loan', (req, res) => res.sendFile(path.join(__dirname, 'public', 'record_loan.html')));
app.get('/repay-loan', (req, res) => res.sendFile(path.join(__dirname, 'public', 'record_repayment.html')));
app.get('/fines', (req, res) => res.sendFile(path.join(__dirname, 'public', 'fines_management.html')));
app.get('/create-caisse', (req, res) => res.sendFile(path.join(__dirname, 'public', 'create_caisse.html')));
app.get('/record-transaction', (req, res) => res.sendFile(path.join(__dirname, 'public', 'record_transaction.html')));
app.get('/record-expenditure', (req, res) => res.sendFile(path.join(__dirname, 'public', 'record_expenditure.html')));
app.get('/distribute-pot', (req, res) => res.sendFile(path.join(__dirname, 'public', 'distribute_pot.html')));
app.get('/journal', (req, res) => res.sendFile(path.join(__dirname, 'public', 'journal_transactions.html')));
app.get('/join', (req, res) => res.sendFile(path.join(__dirname, 'public', 'invitation.html')));

app.listen(PORT, '0.0.0.0', () => {
    console.log(`Cotisation Server is running on http://localhost:${PORT}`);
});
