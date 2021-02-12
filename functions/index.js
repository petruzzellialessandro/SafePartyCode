const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();
const db = admin.firestore();
const BassoRischio = 2;
const AltoRischio = 3;
const Positivo = 4;
const PercentualeDiRischio = 0.3;
let st;

// Create and Deploy Your First Cloud Functions
// https://firebase.google.com/docs/functions/write-firebase-functions

exports.updateUsersStatus = functions.firestore.document("Utenti/{userId}")
  .onUpdate((change, context) => {
    const oldStatus = change.before.data().status;
    const newStatus = change.after.data().status;
    console.log(newStatus, oldStatus);
    if (newStatus !== oldStatus) {
      switch (newStatus) {
        case Positivo: {
          db.collection("Utenti").doc(change.after.id).collection("Contatti")
            .where("da_tracciare", "==", true).get().then((snap) => {
              if (snap.docs.length > 0) {
                snap.docs.forEach((doc) => {
                  console.log(doc.data().id_contatto);
                  db.collection("Utenti").doc(doc.data().id_contatto).get()
                    .then((docContatto) => {
                      if (docContatto.data().status <= AltoRischio) {
                        db.collection("Utenti")
                          .doc(doc.data().id_contatto)
                          .update("status", AltoRischio);
                      }
                    });
                });
              }
            });
          break;
        }
        case AltoRischio: {
          db.collection("Utenti").doc(change.after.id)
            .collection("Contatti")
            .where("da_tracciare", "==", true).get()
            .then((snap) => {
              if (snap.docs.length > 0) {
                snap.docs.forEach((doc) => {
                  db.collection("Utenti").doc(doc.data().id_contatto).get()
                    .then((docB) => {
                      if (docB.data().status <= BassoRischio) {
                        let contattiTracciare = 0;
                        let contattiAltoRischio = 0;
                        db.collection("Utenti").doc(docB.id)
                          .collection("Contatti")
                          .where("da_tracciare", "==", true).get()
                          .then((contattiDiB) => {
                            contattiTracciare = contattiDiB.docs.length;
                            if (contattiDiB.docs.length > 0) {
                              contattiDiB.docs
                                .forEach((conDiB) => {
                                  db.collection("Utenti")
                                    .doc(conDiB.data().id_contatto)
                                    .get()
                                    .then((UConDiB) => {
                                      st = UConDiB.data().status;
                                      if (st == AltoRischio) {
                                        contattiAltoRischio = contattiAltoRischio + 1;
                                      }
                                    }).then(() => {
                                      console.log(contattiAltoRischio, contattiTracciare, contattiAltoRischio / contattiTracciare)
                                      if (contattiTracciare > 0 && ((contattiAltoRischio / contattiTracciare) > PercentualeDiRischio)) {
                                        db.collection("Utenti").doc(doc.data().id_contatto).update("status", BassoRischio);
                                      }
                                    })
                                });
                            }
                          })
                      }
                    })
                })
              }
            }).catch(e => { console.log(e) })
          break;
        }
      }
    }
  });

exports.callUpdateUsersStatus = functions.https.onCall((req, res) => {
  console.log(req)
  const oldStatus = req.OldStatus
  const newStatus = req.NewStatus
  console.log(newStatus, oldStatus);
  if (newStatus !== oldStatus) {
    switch (newStatus) {
      case Positivo: {
        db.collection("Utenti").doc(req.Id).collection("Contatti")
          .where("da_tracciare", "==", true).get().then((snap) => {
            if (snap.docs.length > 0) {
              snap.docs.forEach((doc) => {
                console.log(doc.data().id_contatto);
                db.collection("Utenti").doc(doc.data().id_contatto).get()
                  .then((docContatto) => {
                    if (docContatto.data().status <= AltoRischio) {
                      db.collection("Utenti")
                        .doc(doc.data().id_contatto)
                        .update("status", AltoRischio);
                    }
                  });
              });
            }
          });
        break;
      }
      case AltoRischio: {
        db.collection("Utenti").doc(change.after.id)
          .collection("Contatti")
          .where("da_tracciare", "==", true).get()
          .then((snap) => {
            if (snap.docs.length > 0) {
              snap.docs.forEach((doc) => {
                db.collection("Utenti").doc(doc.data().id_contatto).get()
                  .then((docB) => {
                    if (docB.data().status <= BassoRischio) {
                      let contattiTracciare = 0;
                      let contattiAltoRischio = 0;
                      db.collection("Utenti").doc(docB.id)
                        .collection("Contatti")
                        .where("da_tracciare", "==", true).get()
                        .then((contattiDiB) => {
                          contattiTracciare = contattiDiB.docs.length;
                          if (contattiDiB.docs.length > 0) {
                            contattiDiB.docs
                              .forEach((conDiB) => {
                                db.collection("Utenti")
                                  .doc(conDiB.data().id_contatto)
                                  .get()
                                  .then((UConDiB) => {
                                    st = UConDiB.data().status;
                                    if (st == AltoRischio) {
                                      contattiAltoRischio = contattiAltoRischio + 1;
                                    }
                                  }).then(() => {
                                    console.log(contattiAltoRischio, contattiTracciare, contattiAltoRischio / contattiTracciare)
                                    if (contattiTracciare > 0 && ((contattiAltoRischio / contattiTracciare) > PercentualeDiRischio)) {
                                      db.collection("Utenti").doc(doc.data().id_contatto).update("status", BassoRischio);
                                    }
                                  })
                              });
                          }
                        })
                    }
                  })
              })
            }
          }).catch(e => { console.log(e) })
        break;
      }
    }

  }
})