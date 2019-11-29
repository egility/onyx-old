/*
 * Copyright (c) Mike Brickman 2014-2017
 */


var routing = require("../lib/routing");

routing.add('/', {redirect: '/index'});

routing.add('/api/*', {handover: "/api"});

routing.add('/agilitynet');

routing.add('/index');
routing.add('/index_authenticated', {api: '/competitor/session/~token?authenticated', redirect: '/'});
routing.add('/index_switched', {api: '/competitor/session/~token?authenticated', redirect: '/household'});
routing.add('/index/not_registered', {api: '/competitor/session/~token?authenticated', render: '/not_registered'});
routing.add('/revert_reverted', {redirect: '/search'});

routing.add('/competitor_add', {api: '/competitor/add'});


routing.add('/logout', {session: 'clear', redirect: '/'});
routing.add('/revert', {api: "/competitor/revert", redirect: '/search'});
routing.add('/competitor/edit/:altIdCompetitor', {
    api: '/competitor/:altIdCompetitor/session',
    redirect: '/competitor_edit'
});
routing.add('/competitor_edit', {api: '/competitor/@altIdCompetitor?select=*'});
routing.add('/competitor_edit/done', {redirect: '/household'});
routing.add('/competitor_edit/email_changed', {api: '/competitor/@idCompetitor?unVerifiedEmail=true'});
routing.add('/competitor_email_verified', {api: '/competitor/session/~token'});


routing.add('/household', {api: '/account/@idAccount/all'});
routing.add('/household_edit', {api: '/account/@idAccount'});
routing.add('/household_edit/done', {redirect: '/household'});
routing.add('/household/add_member', {
    api: '/account/@idAccount/competitors/new?select=*',
    render: '/household_add_member'
});
routing.add('/household/add_member/done', {redirect: '/household'});

routing.add('/household/add_handler', {api: '/account/@idAccount/handler/new', render: '/household_add_handler'});
routing.add('/household/add_handler/done', {redirect: '/household'});


routing.add('/account', {api: '/account/@idAccount/ledger'});

routing.add('/account/done', {redirect: '/account'});
routing.add('/payment_card', {api: '/competitor/@idCompetitor/paymentCards/add'});
routing.add('/payment_card/done', {redirect: '/account'});

routing.add('/refund', {api: '/account/@idAccount/refund'});
routing.add('/refund/done', {api: '/account/@idAccount/refund'});
routing.add('/refund/confirm');
routing.add('/refund_confirmed', {api: '/account/@idAccount/refund/confirmed/~token'});


// Dogs
routing.add('/dog/review', {api: '/dog/@idDog?select=*', render: '/dog'});
routing.add('/dog/edit', {api: '/dog/@idDog?select=*', render: '/dog_edit'});
routing.add('/dog/edit/done', {redirect: '/dog/review'});
routing.add('/dog/edit/kc_grade_review', {api: '/dog/@idDog/kc_grade_review', render: '/dog_kc_grade_review'});
routing.add('/dog/edit/kc_grade_review/done', {api: '/dog/@idDog/kc_grade_change', render: '/dog_kc_grade_confirmed'});

routing.add('/dog/points', {api: '/dog/@idDog/ukaprogress', render: '/dog_points'});
routing.add('/dog/results', {api: '/dog/@idDog/results', render: '/dog_results'});

routing.add('/dog/edit_uka', {api: '/dog/@idDog/organization/2', render: '/dog_edit_uka'});
routing.add('/dog/edit_uka/done', {redirect: '/dog/review'});


routing.add('/dog/add', {api: '/account/@idAccount/dogs/new?select=*', render: '/dog_add'});
routing.add('/dog/add/done', {redirect: '/household?root=true'});

routing.add('/dog/select/:idDog', {api: '/dog/:idDog/session', redirect: '/dog/~destination'});

routing.add('/dog/share_with', {api: '/dog/@idDog?share', render: '/dog_share_with'});
routing.add('/dog/share_with/done', {redirect: '/dog/review'});


routing.add('/register', {api: '/competitor/register'});
routing.add('/register/register_new', {redirect: '/register_new_verification_email_sent?email=~email'});
routing.add('/register/register_uka', {api: '/competitor/session/~token', redirect: '/register_uka_confirm_email'});

// registration using UKA data
routing.add('/register_uka_confirm_email', {api: '/competitor/@idCompetitor/email'});
routing.add('/register_uka_confirm_email/done', {redirect: 'register_uka_verification_email_sent?email=~email'});
routing.add('/register_uka_verification_email_sent');

routing.add('/register_uka_email_verified', {
    api: '/competitor/session/~token?registrationComplete',
    redirect: '/register_uka_password'
});

routing.add('/register_uka', {
    api: '/competitor/session/~token?registrationComplete',
    redirect: '/register_uka_password'
});
routing.add('/register_uka_password', {api: '/competitor/@idCompetitor?set_password=true'});
routing.add('/register_uka_password/done', {redirect: '/register_uka_dogs'});
routing.add('/register_uka_dogs', {api: '/competitor/@idCompetitor/dogs?registering=true'});
routing.add('/register_uka_dogs/done', {redirect: '/register_uka_kc_info'});
routing.add('/register_uka_kc_info', {api: '/competitor/@idCompetitor/dogs?registering=true'});
routing.add('/register_uka_kc_info/done', {redirect: '/register_welcome'});
routing.add('/register_welcome', {api: '/competitor/@idCompetitor?registrationComplete=true'});


// registration using new data
routing.add('/register_new_verification_email_sent');
routing.add('/register_new_email_verified', {api: '/competitor/session/~token', redirect: '/register_new_complete'});
routing.add('/register_new_complete', {api: '/competitor/@idCompetitor'});
routing.add('/register_new_complete/done', {redirect: '/register_welcome'});

//shows
routing.add('/live');

routing.add('/live', {redirect: '/competition/live?root=true'});
routing.add('/competition/active', {api: '/competition/active', render: '/competitions'});
routing.add('/competition/live', {api: '/competition/live', render: '/competitions_live'});
routing.add('/competition/open', {api: '/competition/open', render: '/competitions_open'});
routing.add('/competition/open_paper', {api: '/competition/open?paper', render: '/competitions_open_paper'});
routing.add('/competition/entries', {api: '/account/@idAccount/entries', render: '/competitions_entered'});

routing.add('/competition/switchboards', {
    api: '/competitor/@idCompetitor/competitions',
    render: '/competitions_switchboard'
});

routing.add('/kc_show/list', {api: '/kc_show/list', render: '/kc_shows'});
routing.add('/kc_show/map', {api: '/kc_show/map', render: '/google_map'});
routing.add('/agilitynet/map', {api: '/agilitynet/map', render: '/google_map'});
routing.add('/agilitynet/target/map', {api: '/agilitynet/map?target', render: '/google_map'});
routing.add('/competition/active/map', {api: '/competition/active/map', render: '/google_map'});
routing.add('/account/map/postcode/:postcodes', {api: '/account/map?postcodes=:postcodes', render: '/google_map'});



routing.add('/competition/:idCompetition/map', {api: '/competition/:idCompetition/map', render: '/google_map' });


routing.add('/results', {api: '/competition/results'});


routing.add('/competition_kc/:idCompetition', {
    api: '/competition/:idCompetition?switchboard',
    render: '/competition_kc'
});
routing.add('/competition_uka/:idCompetition', {api: '/competition/:idCompetition', render: '/competition_uka'});
routing.add('/competition_uk_open/:idCompetition', {
    api: '/competition/:idCompetition',
    render: '/competition_uk_open'
});
routing.add('/competition_fab/:idCompetition', {api: '/competition/:idCompetition', render: '/competition_fab'});
routing.add('/competition_ind/:idCompetition', {api: '/competition/:idCompetition', render: '/competition_ind'});

routing.add('/competition/:idCompetition/ringPlan/:date', {
    api: '/competition/:idCompetition/ringPlanDynamic/:date',
    render: '/ring_plan'
});

routing.add('/competition/:idCompetition/ringPlan2/:date', {
    api: '/competition/:idCompetition/ringPlanDynamic/:date',
    render: '/ring_plan2'
});

routing.add('/competition/:idCompetition/display/:date', {
    api: '/competition/:idCompetition/ringPlanDynamic/:date',
    render: '/ring_plan_display'
});

routing.add('/competition/:idCompetition/entries_uk_open', {
    api: '/competition/:idCompetition/entries',
    render: '/competition_uk_open_entries'
});

routing.add('/competition/:idCompetition/camping_uk_open', {
    api: '/competition/:idCompetition/camping',
    render: '/competition_uk_open_camping'
});

routing.add('/competition/:idCompetition/entries_uka', {
    api: '/competition/:idCompetition/entries',
    render: '/competition_uka_entries'
});

routing.add('/competition/:idCompetition/entry_stats', {
    api: '/competition/:idCompetition/entryStats',
    render: '/competition_entry_stats'
});

routing.add('/competition/:idCompetition/entries_kc', {
    api: '/competition/:idCompetition/entries',
    render: '/competition_kc_entries'
});

routing.add('/competition/:idCompetition/deleteKcUnpaid', {
    api: '/competition/:idCompetition/deleteUnpaid',
    redirect: '/competition/:idCompetition/entries_kc'
});

routing.add('/competition/:idCompetition/camping', {
    api: '/competition/:idCompetition/camping',
    render: '/competition_camping'
});

routing.add('/competition/:idCompetition/campingWaiting', {
    api: '/competition/:idCompetition/camping?waiting',
    render: '/competition_camping_waiting'
});

routing.add('/competition/:idCompetition/campingWaiting/done', {
    redirect: '/competition/:idCompetition/camping'
});

routing.add('/competition/:idCompetition/helpers_kc', {
    api: '/competition/:idCompetition/helpers',
    render: '/competition_kc_helpers'
});

routing.add('/competition/:idCompetition/switchboard_kc', {
    api: '/competition/:idCompetition?switchboard',
    render: '/switchboard_kc'
});
routing.add('/competition/:idCompetition/switchboard_uka', {
    api: '/competition/:idCompetition?switchboard',
    render: '/switchboard_uka'
});
routing.add('/competition/:idCompetition/switchboard_uk_open', {
    api: '/competition/:idCompetition?switchboard',
    render: '/switchboard_uk_open'
});
routing.add('/competition/:idCompetition/switchboard_fab', {
    api: '/competition/:idCompetition?switchboard',
    render: '/switchboard_fab'
});
routing.add('/competition/:idCompetition/switchboard_ind', {
    api: '/competition/:idCompetition?switchboard',
    render: '/switchboard_ind'
});

routing.add('/competition/:idCompetition/switchboard_kc/done', {redirect: '/competition/:idCompetition/switchboard_kc'});
routing.add('/competition/:idCompetition/switchboard_ind/done', {redirect: '/competition/:idCompetition/switchboard_ind'});

routing.add('/competition/:idCompetition/sign_on', {
    api: '/competition/:idCompetition/signOn',
    render: '/competition_sign_on'
});

routing.add('/competition/:idCompetition/tablets', {
    api: '/competition/:idCompetition/tablet',
    render: '/competition_tablet'
});

routing.add('/competition/:idCompetition/signal', {
    api: '/competition/:idCompetition/signal',
    render: '/competition_dongle'
});

routing.add('/competition/:idCompetition/summary/:date', {
    api: '/competition/:idCompetition/ringPlan/:date?summary',
    render: '/ring_plan_summary'
});

routing.add('/competition/:idCompetition/cancel', {
    api: '/account/@idAccount/competition/:idCompetition/cancel',
    render: '/entry_cancel'
});

routing.add('/competition/:idCompetition/cancel/done', {redirect: '/competition/entries?root=true'});


routing.add('/results/:year', {api: '/competition/results?year=:year', render: '/results'});
routing.add('/results', {api: '/competition/results'});


routing.add('/competition/:idCompetition/results', {
    api: '/competition/:idCompetition/results',
    render: '/competition_results'
});

routing.add('/agilityClass/:idAgilityClass/results', {
    api: '/agilityClass/:idAgilityClass/results',
    render: '/agility_class_results'
});

routing.add('/agilityClass/:idAgilityClass/dog_results', {
    api: '/agilityClass/:idAgilityClass/results?dog=@idDog',
    render: '/agility_class_results'
});


routing.add('/clubs');
routing.add('/events');
routing.add('/products');
routing.add('/about');
routing.add('/contact');
routing.add('/terms');
routing.add('/privacy');
routing.add('/faq', {api: '/account/@idAccount'});

routing.add('/error');

routing.add('/test', {render: "/_test"});


routing.add('/system_switchboard');
routing.add('/system_switchboard/accounting_system', {redirect: '/ledger_accounts?root=true'});
routing.add('/payment_requests', {api: '/payment_requests'});

/***********************************************************************************************/
/*                             Review Show Entry                                               */
/***********************************************************************************************/

routing.add('/competition/:idCompetition/review', {
    api: '/account/@idAccount/competition/:idCompetition',
    render: '/entry_review'
});

routing.add('/competition/:idCompetition/review_uk_open', {
    api: '/account/@idAccount/competition/:idCompetition',
    render: '/entry_review_uk_open'
});


routing.add('/competition/:idCompetition/transfer_camping', {
    api: '/account/@idAccount/competition/:idCompetition/transfer_camping',
    render: '/transfer_camping'
});

routing.add('/competition/:idCompetition/transfer_camping/done', {redirect: '/entry_review'});

/***********************************************************************************************/
/*                             Enter FAB Show                                                  */
/***********************************************************************************************/

routing.add('/competition/:idCompetition/enter_fab', {
    api: '/account/@idAccount/competition/:idCompetition/reset',
    redirect: '/competition/:idCompetition/preferences_fab'
});

routing.add('/competition/:idCompetition/enter_fab_paper', {
    api: '/account/@idAccount/competition/:idCompetition/reset?paper',
    redirect: '/competition/:idCompetition/preferences_fab'
});

routing.add('/competition/:idCompetition/preferences_fab', {
    api: '/account/@idAccount/competition/:idCompetition/dogs',
    render: '/fab_show_preferences'
});

routing.add('/competition/:idCompetition/preferences_fab/done', {redirect: '/competition/:idCompetition/enter_classes_fab'});

routing.add('/competition/:idCompetition/preferences_fab/select/:idDog', {
    api: '/dog/:idDog/session',
    redirect: '/competition/:idCompetition/preferences_fab/dog'
});

routing.add('/competition/:idCompetition/preferences_fab/dog', {
    api: '/account/@idAccount/competition/:idCompetition/dogs/@idDog',
    render: '/dog_edit_preference_fab'
});

routing.add('/competition/:idCompetition/preferences_fab/dog/done', {
    redirect: '/competition/:idCompetition/preferences_fab'
});

routing.add('/competition/:idCompetition/enter_classes_fab', {
    api: '/account/@idAccount/competition/:idCompetition/entry',
    render: '/fab_show_enter'
});

routing.add('/competition/:idCompetition/enter_classes_fab/done', {redirect: '/competition/:idCompetition/help_fab'});

routing.add('/competition/:idCompetition/help_fab', {
    api: '/account/@idAccount/competition/:idCompetition/competitors',
    render: '/fab_show_help'
});

routing.add('/competition/:idCompetition/help_fab/done', {redirect: '/competition/:idCompetition/checkout_fab'});

routing.add('/competition/:idCompetition/checkout_fab', {
    api: '/account/@idAccount/competition/:idCompetition/checkout',
    render: '/fab_show_checkout'
});

routing.add('/competition/:idCompetition/checkout_fab/done', {redirect: '/competition/:idCompetition/confirmed'});

routing.add('/competition/:idCompetition/checkout_fab/quit', {redirect: '/competition/entries?root=true'});


/***********************************************************************************************/
/*                             Enter IND Show                                                  */
/***********************************************************************************************/

routing.add('/competition/:idCompetition/enter_ind', {
    api: '/account/@idAccount/competition/:idCompetition/reset',
    redirect: '/competition/:idCompetition/preferences_ind'
});

routing.add('/competition/:idCompetition/enter_ind_paper', {
    api: '/account/@idAccount/competition/:idCompetition/reset?paper',
    redirect: '/competition/:idCompetition/preferences_ind'
});

routing.add('/competition/:idCompetition/preferences_ind', {
    api: '/account/@idAccount/competition/:idCompetition/dogs',
    render: '/ind_show_preferences'
});

routing.add('/competition/:idCompetition/preferences_ind/done', {redirect: '/competition/:idCompetition/enter_classes_ind'});

routing.add('/competition/:idCompetition/preferences_ind/select/:idDog', {
    api: '/dog/:idDog/session',
    redirect: '/competition/:idCompetition/preferences_ind/dog'
});

routing.add('/competition/:idCompetition/preferences_ind/dog', {
    api: '/account/@idAccount/competition/:idCompetition/dogs/@idDog',
    render: '/dog_edit_preference_ind'
});

routing.add('/competition/:idCompetition/preferences_ind/dog/done', {
    redirect: '/competition/:idCompetition/preferences_ind'
});

routing.add('/competition/:idCompetition/enter_classes_ind', {
    api: '/account/@idAccount/competition/:idCompetition/entry',
    render: '/ind_show_enter'
});

routing.add('/competition/:idCompetition/enter_classes_ind/done', {redirect: '/competition/:idCompetition/help_ind'});

routing.add('/competition/:idCompetition/enter_classes_ind/supplementary', {
    api: '/account/@idAccount/competition/:idCompetition/supplementary',
    render: '/ind_show_supplementary'
});

routing.add('/competition/:idCompetition/enter_classes_ind/supplementary/done', {redirect: '/competition/:idCompetition/help_ind'});


routing.add('/competition/:idCompetition/help_ind', {
    api: '/account/@idAccount/competition/:idCompetition/competitors',
    render: '/ind_show_help'
});

routing.add('/competition/:idCompetition/help_ind/done', {redirect: '/competition/:idCompetition/checkout_ind'});

routing.add('/competition/:idCompetition/checkout_ind', {
    api: '/account/@idAccount/competition/:idCompetition/checkout',
    render: '/ind_show_checkout'
});

routing.add('/competition/:idCompetition/checkout_ind/done', {redirect: '/competition/:idCompetition/confirmed'});

routing.add('/competition/:idCompetition/checkout_ind/quit', {redirect: '/competition/entries?root=true'});

/***********************************************************************************************/
/*                             Enter IND_AA Show                                                  */
/***********************************************************************************************/

routing.add('/competition/:idCompetition/enter_ind_aa', {
    api: '/account/@idAccount/competition/:idCompetition/reset',
    redirect: '/competition/:idCompetition/preferences_ind_aa'
});

routing.add('/competition/:idCompetition/enter_ind_aa_paper', {
    api: '/account/@idAccount/competition/:idCompetition/reset?paper',
    redirect: '/competition/:idCompetition/preferences_ind_aa'
});

routing.add('/competition/:idCompetition/preferences_ind_aa', {
    api: '/account/@idAccount/competition/:idCompetition/dogs',
    render: '/ind_aa_show_preferences'
});

routing.add('/competition/:idCompetition/preferences_ind_aa/done', {redirect: '/competition/:idCompetition/checkout_ind'});


/***********************************************************************************************/
/*                             Enter UKA Show                                                  */
/***********************************************************************************************/

routing.add('/competition/:idCompetition/enter_uka', {
    api: '/account/@idAccount/competition/:idCompetition/reset',
    redirect: '/competition/:idCompetition/preferences_uka'
});

routing.add('/competition/:idCompetition/enter_uka_paper', {
    api: '/account/@idAccount/competition/:idCompetition/reset?paper',
    redirect: '/competition/:idCompetition/preferences_uka'
});

routing.add('/competition/:idCompetition/preferences_uka', {
    api: '/account/@idAccount/competition/:idCompetition/dogs',
    render: '/uka_show_preferences'
});

routing.add('/competition/:idCompetition/preferences_uka/done', {redirect: '/competition/:idCompetition/enter_classes_uka'});

routing.add('/competition/:idCompetition/preferences_uka/select/:idDog', {
    api: '/dog/:idDog/session',
    redirect: '/competition/:idCompetition/preferences_uka/dog'
});

routing.add('/competition/:idCompetition/preferences_uka/dog', {
    api: '/account/@idAccount/competition/:idCompetition/dogs/@idDog',
    render: '/dog_edit_preference_uka'
});

routing.add('/competition/:idCompetition/preferences_uka/dog/done', {
    redirect: '/competition/:idCompetition/preferences_uka'
});


routing.add('/competition/:idCompetition/enter_classes_uka', {
    api: '/account/@idAccount/competition/:idCompetition/entry',
    render: '/uka_show_enter'
});

routing.add('/competition/:idCompetition/enter_classes_uka/done', {redirect: '/competition/:idCompetition/help_uka'});

routing.add('/competition/:idCompetition/enter_classes_uka_supplementary', {
    api: '/account/@idAccount/competition/:idCompetition/supplementary',
    render: '/uka_show_supplementary'
});

routing.add('/competition/:idCompetition/enter_classes_uka_finals_supplementary', {
    api: '/account/@idAccount/competition/:idCompetition/supplementary',
    render: '/uka_show_supplementary'
});

routing.add('/competition/:idCompetition/enter_classes_uka_supplementary/done', {redirect: '/competition/:idCompetition/help_uka'});


routing.add('/competition/:idCompetition/help_uka', {
    api: '/account/@idAccount/competition/:idCompetition/competitors',
    render: '/uka_show_help'
});

routing.add('/competition/:idCompetition/help_uka/done', {redirect: '/competition/:idCompetition/checkout_uka'});

routing.add('/competition/:idCompetition/checkout_uka', {
    api: '/account/@idAccount/competition/:idCompetition/checkout',
    render: '/uka_show_checkout'
});

//routing.add('/competition/:idCompetition/checkout_uka/done', {redirect: '/account'});
routing.add('/competition/:idCompetition/checkout_uka/done', {redirect: '/competition/:idCompetition/confirmed'});

routing.add('/competition/:idCompetition/checkout_uka/quit', {redirect: '/competition/entries?root=true'});


/**********/

routing.add('/competition/:idCompetition/enter_uka_finals', {
    api: '/account/@idAccount/competition/:idCompetition/reset',
    redirect: '/competition/:idCompetition/enter_classes_uka_finals'
});

routing.add('/competition/:idCompetition/enter_uka_paper_finals', {
    api: '/account/@idAccount/competition/:idCompetition/reset?paper',
    redirect: '/competition/:idCompetition/enter_classes_uka_finals'
});

routing.add('/competition/:idCompetition/enter_classes_uka_finals', {
    api: '/account/@idAccount/competition/:idCompetition/entry',
    render: '/uka_show_enter'
});

routing.add('/competition/:idCompetition/enter_classes_uka_finals/done', {redirect: '/competition/:idCompetition/checkout_uka'});

/***********************************************************************************************/
/*                             Enter UK Open                                                   */
/***********************************************************************************************/

routing.add('/competition/:idCompetition/enter_uk_open', {
    api: '/account/@idAccount/competition/:idCompetition/reset',
    redirect: '/competition/:idCompetition/uk_open_enter'
});

routing.add('/competition/:idCompetition/enter_uk_open_paper', {
    api: '/account/@idAccount/competition/:idCompetition/reset?paper',
    redirect: '/competition/:idCompetition/uk_open_enter'
});

routing.add('/competition/:idCompetition/uk_open_enter', {
    api: '/account/@idAccount/competition/:idCompetition/transaction',
    render: 'uk_open_enter'
});

routing.add('/competition/:idCompetition/uk_open_enter/done', {redirect: '/competition/:idCompetition/checkout_uk_open'});

routing.add('/competition/:idCompetition/checkout_uk_open', {
    api: '/account/@idAccount/competition/:idCompetition/checkout',
    render: '/uk_open_checkout'
});

routing.add('/competition/:idCompetition/checkout_uk_open/done', {redirect: '/account'});
routing.add('/competition/:idCompetition/checkout_uk_open/quit', {redirect: '/competition/entries?root=true'});


/***********************************************************************************************/
/*                             Enter KC Show                                                   */
/***********************************************************************************************/


routing.add('/competition/:idCompetition/enter_kc', {
    api: '/account/@idAccount/competition/:idCompetition/reset',
    redirect: '/competition/:idCompetition/preferences_kc'
});

routing.add('/competition/:idCompetition/enter_kc_paper', {
    api: '/account/@idAccount/competition/:idCompetition/reset?paper',
    redirect: '/competition/:idCompetition/preferences_kc'
});

routing.add('/competition/:idCompetition/preferences_kc', {
    api: '/account/@idAccount/competition/:idCompetition/dogs',
    render: '/kc_show_preferences'
});

routing.add('/competition/:idCompetition/preferences_kc/done', {redirect: '/competition/:idCompetition/enter_classes_kc'});

routing.add('/competition/:idCompetition/enter_classes_kc', {
    api: '/account/@idAccount/competition/:idCompetition/entry',
    render: '/kc_show_enter'
});

routing.add('/competition/:idCompetition/enter_classes_kc/done', {redirect: '/competition/:idCompetition/help_kc'});

routing.add('/competition/:idCompetition/enter_classes_kc_supplementary', {
    api: '/account/@idAccount/competition/:idCompetition/supplementary',
    render: '/kc_show_supplementary'
});

routing.add('/competition/:idCompetition/enter_classes_kc_supplementary/done', {redirect: '/competition/:idCompetition/help_kc'});


routing.add('/competition/:idCompetition/help_kc', {
    api: '/account/@idAccount/competition/:idCompetition/competitors',
    render: '/kc_show_help'
});

routing.add('/competition/:idCompetition/help_kc/done', {redirect: '/competition/:idCompetition/checkout_kc'});

routing.add('/competition/:idCompetition/checkout_kc', {
    api: '/account/@idAccount/competition/:idCompetition/checkout',
    render: '/kc_show_checkout'
});


routing.add('/competition/:idCompetition/checkout_kc/done', {redirect: '/competition/:idCompetition/confirmed'});
routing.add('/competition/:idCompetition/checkout_kc/quit', {redirect: '/competition/entries?root=true'});

routing.add('/competition/:idCompetition/confirmed', {
    api: '/account/@idAccount/competition/:idCompetition',
    render: '/entry_confirmed'
});
routing.add('/competition/:idCompetition/confirmed/done', {redirect: '/account'});


routing.add('/password_forgotten', {api: '/request_reset_password'});
routing.add('/password_forgotten/done', {redirect: '/password_forgotten_email_sent?email=~email'});
routing.add('/password_forgotten_email_sent');

routing.add('/password_reset', {api: '/reset_password/~token'});
routing.add('/password_reset/done', {render: '/password_reset_done'});


routing.add('/account_error');


routing.add('/ledger_accounts', {api: '/ledgerAccount/overview'});
routing.add('/ledger_account/:idLedgerAccount', {api: '/ledgerAccount/:idLedgerAccount', render: '/ledger_account'});
routing.add('/competition_accounts/:idCompetition', {
    api: '/ledgerAccount/overview?idCompetition=:idCompetition',
    render: 'competition_accounts'
});
routing.add('/competition_account/:idCompetition/:idLedgerAccount', {
    api: '/ledgerAccount/:idLedgerAccount?idCompetition=:idCompetition',
    render: '/competition_account'
});


routing.add('/search');

routing.add('/account/list', {api: '/account/list?$', render: '/account_list'});


routing.add('/tuffley_camping', {api: '/competition/1170660471/camping/request'});
routing.add('/tuffley_camping/done');

routing.add('/uka_membership', {api: '/account/@idAccount/uka'});
routing.add('/uka_membership/done', {redirect: '/uka_membership_checkout'});
routing.add('/uka_membership_checkout', {api: '/account/@idAccount/ukaCheckout'});
routing.add('/uka_membership_checkout/done', {redirect: '/account'});
routing.add('/uka_membership_checkout/paid', {redirect: '/household'});

routing.add('/uka_ledger/:altIdCompetitor', {api: '/competitor/:altIdCompetitor/session', redirect: '/uka_ledger'});
routing.add('/uka_ledger', {api: '/competitor/@altIdCompetitor/ukaLedger'});


routing.add('/cancel_camping_request', {api: '/cancel_camping_request/~token'});
routing.add('/cancel_camping_request/done');

routing.add('/class_codes', {api: '/classCode/list'});

routing.add('/voucher', {api: '/competition/~idCompetition/voucher/~voucherCode', render: 'voucher'});
routing.add('/voucher/done', {redirect: '/competition/~idCompetition/switchboard_kc'});


routing.add('/magazine/*');

routing.add('/acu_masters', {api: '/replication', render: 'acu_masters'});
routing.add('/acu_list/:idAcu', {api: '/acu/:idAcu', render: 'acu_list'});
routing.add('/acu_diagnostics/:idAcu', {api: '/acu/:idAcu/diagnostics', render: 'acu_diagnostics'});

routing.add('/uka_junior_league', {api: '/uka/junior_league'});

routing.add('/missing_payment', {api: '/unallocatedReceipts'});
routing.add('/missing_payment/done', {redirect: '/account'});


routing.add('/dongles', {api: '/dongle/list', render: '/dongle'});
routing.add('/tablets', {api: '/tablet/list', render: '/tablets'});
routing.add('/tablets_in_use', {api: '/tablet/inUse', render: '/tablets_in_use'});
routing.add('/uka_tablets', {api: '/tablet/inUse?uka', render: '/tablets_in_use'});
routing.add('/acus', {api: '/acu/list', render: '/acus'});
routing.add('/tablet/:idDevice', {api: '/tablet/:idDevice', render: '/tablet_log'});
routing.add('/sign_on', {api: '/signOn', render: '/sign_on'});

routing.add('/team/:idTeam/crufts_team', {api: '/team/:idTeam', render: '/kc_crufts_team'});
routing.add('/team/:idTeam/crufts_team/done', {redirect: '/team/:idTeam/crufts_team'});

routing.add('/competition/:idCompetition/crufts_teams', {
    api: '/competition/:idCompetition/cruftsTeams',
    render: '/competition_crufts_teams'
});

routing.add('/competition/:idCompetition/team/:idTeam/crufts_team', {api: '/team/:idTeam', render: '/kc_crufts_team'});
routing.add('/competition/:idCompetition/team/:idTeam/crufts_team/done', {redirect: '/competition/:idCompetition/crufts_teams'});

routing.add('/stock_movement', {api: '/stock/movement', render: '/stock_movement'});
routing.add('/stock_movement/done', {redirect: '/stock_movement_confirm'});
routing.add('/stock_movement_confirm', {api: '/stock/movement/confirm', render: '/stock_movement_confirm'});
routing.add('/stock_movement_confirm/done', {redirect: '/system_switchboard'});

routing.add('/stock_list', {api: '/stock/list', render: '/stock_list'});

routing.add('/competition/:idCompetition/stock_parameters', {
    api: '/competition/:idCompetition/stock',
    render: '/competition_stock_parameters'
});
routing.add('/competition/:idCompetition/stock_parameters/doneKc', {redirect: '/competition/:idCompetition/switchboard_kc'});
routing.add('/competition/:idCompetition/stock_parameters/doneFab', {redirect: '/competition/:idCompetition/switchboard_fab'});

routing.add('/competition/:idCompetition/stock_picking_list', {
    api: '/competition/:idCompetition/stock',
    render: '/competition_stock_picking_list'
});

routing.add('/competition/:idCompetition/stock_out', {
    api: '/competition/:idCompetition/stock_out',
    render: '/stock_movement'
});
routing.add('/competition/:idCompetition/stock_out/done', {redirect: '/competition/:idCompetition/stock_out_confirm'});
routing.add('/competition/:idCompetition/stock_out_confirm', {
    api: '/competition/:idCompetition/stock_out/confirm',
    render: '/stock_movement_confirm'
});
routing.add('/competition/:idCompetition/stock_out_confirm/doneKc', {redirect: '/competition/:idCompetition/switchboard_kc'});
routing.add('/competition/:idCompetition/stock_out_confirm/doneFab', {redirect: '/competition/:idCompetition/switchboard_fab'});

routing.add('/emails', {api: '/account/@idAccount/emails/list', render: '/emails'});
routing.add('/email/:idEmailQueue', {api: '/account/@idAccount/emails/:idEmailQueue', render: '/email'});

routing.add('/split_from_account', {api: '/competitor/@idCompetitor/split', redirect: '/household'});

routing.add('/entities/add', {api: '/entity/new', render: '/entity'});
routing.add('/entities/add/done', {redirect: '/entities'});

routing.add('/entities', {api: '/entity/list'});
routing.add('/entity/:idEntity', {api: '/entity/:idEntity', render: '/entity'});
routing.add('/entity/:idEntity/done', {redirect: '/entities'});
routing.add('/entity/:idEntity/official/add', {api: '/entity/:idEntity/official/new', render: '/entity_official'});
routing.add('/entity/:idEntity/official/add/done', {redirect: '/entity/:idEntity'});
routing.add('/entity/:idEntity/official/:idEntityOfficial', {api: '/entityOfficial/:idEntityOfficial', render: '/entity_official'});
routing.add('/entity/:idEntity/official/:idEntityOfficial/done', {redirect: '/entity/:idEntity'});

routing.add('/entities/marketing', {api: '/entity/list?priority', render: 'entities_marketing'});
routing.add('/entity/marketing/:idEntity', {api: '/entity/:idEntity', render: '/entity'});
routing.add('/entity/marketing/:idEntity/done', {redirect: '/entities/marketing'});
routing.add('/entity/marketing/:idEntity/official/add', {api: '/entity/:idEntity/official/new', render: '/entity_official'});
routing.add('/entity/marketing/:idEntity/official/add/done', {redirect: '/entity/marketing/:idEntity'});
routing.add('/entity/marketing/:idEntity/official/:idEntityOfficial', {api: '/entityOfficial/:idEntityOfficial', render: '/entity_official'});
routing.add('/entity/marketing/:idEntity/official/:idEntityOfficial/done', {redirect: '/entity/marketing/:idEntity'});



