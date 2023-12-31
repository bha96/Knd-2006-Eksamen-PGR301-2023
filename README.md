# Knd-2006-Eksamen-PGR301-2023

Note: Det er en del failed builds på slutten grunnet apprunner tull, men den har vært oppe og kjørt på apprunner tidligere og har ikke endret noe i koden som gjør at den plutselig skal feile.
 
Du vil se at det lokale imaget kjører uten problemer.

Note2: Siden kjell og java-koden egentlig er to forskjellige prosjekter har jeg satt opp i action fila at jobbene kun kjører hvis kode som omhandler dem blir endret.

## Oppgave 1
## A. SAM & GitHub actions workflow
For å få github action til å kjøre på din fork må du legge inn, det finner du på din bruker på IAM inne i aws.
 - AWS_ACCESS_KEY_ID
 - AWS_SECRET_ACCESS_KEY

som secrets. Det finner du her: github.<span>com/"DITT BRUKERNAVN"/"DITT REPO"/settings/secrets/actions
Du trenger også brukernavn og en github token når du skal pushe til forken din. Det finner du [her](https://github.com/settings/tokens/new). Token trenger repo og workflow.
Så kan du gjøre en liten endring i koden som å legge inn en kommentar et sted. Deretter:
 - git add "din fil" 
 - git commit -m"din melding"
 - git push
 
Så kan du gå inn i actions på repositoriet og se at actionen kjører.
I steget "Sam deploy" vil du få en link til funksjonen. MERK: denne går gjennom en gateway som har en maks timout på 29 sekunder, når jeg testet ble funksjonen noen ganger ferdig før og andre ikke. 
Prøvde å sette timeouten på lambdaen til 5 min, men det hjalp ikke på gatewayen forståelig nok.. Under er en direkte link til lambdaen samt apiet gjennom gatewayen. 

API endpoint (gjennom gateway): [Link](https://tvf1d8yjia.execute-api.eu-west-1.amazonaws.com/Prod/hello)

Direkte link til lambda: [Link](https://gcwivjsicibb7jvi4urcicgnze0rdtrd.lambda-url.eu-west-1.on.aws/)

## B. Docker container.
Funker som den skal, du kan også bruke min bucket:

docker run -e AWS_ACCESS_KEY_ID=XXX -e AWS_SECRET_ACCESS_KEY=YYY -e BUCKET_NAME=kandidat2006 kjellpy

## Oppgave 2
## A. Dockerfile

Kjør med dine aws credentials: 
 - docker build -t ppe . 
 - docker run -p 8080:8080 -e AWS_ACCESS_KEY_ID=XXX -e AWS_SECRET_ACCESS_KEY=YYY -e BUCKET_NAME=kjellsimagebucket ppe

eller min bucket:
 - docker run -p 8080:8080 -e AWS_ACCESS_KEY_ID=XXX -e AWS_SECRET_ACCESS_KEY=YYY -e BUCKET_NAME=kandidat2006 ppe

test applikasjon:
 - curl localhost:8080/scan-ppe?bucketName=kjellsimagebucket

eller min:
 - curl localhost:8080/scan-ppe?bucketName=kandidat2006

## B. Actions -> ecr
![screenshot av ecr images hvor den nyeste har tagen "latest"](/img/ecr_screenshot.png)

## Oppgave 3
## A. 
Har forbedret koden ved å trekke ut hardkodede verdier i variabler, og gitt nesten alle defaults utenom hvilket image som skal kjøres. Dette inputter jeg i workflowfilen.

## B.
Jeg husket ikke helt hvilken bucket vi brukte i forelesningen så lagde bare min egen for å bruke som backend til terraform.

For å få action til å kjøre hos deg må du ha AWS_ACCESS_KEY_ID og AWS_SECRET_ACCESS_KEY i secretsene dine. Deretter en liten kommentar et sted i koden for så å:
 - git add "din fil" 
 - git commit -m"din melding"
 - git push
 
Så vil du se at den kjører i actions taben.

## Oppgave 4
## A.
Jeg har valgt timer, meter og gauge som mine metrics. Gaugene er for å se forskjellig data om bucketen min og bildene som ligger der.

Timer er for å se hvor mye tid i gjennomsnitt mine endpoints bruker fra start til slutt.

Meter er for å se hvilke personer og hvor mange ganger de har blitt gjennkjent. Den lager dynamisk en ny bar for hver kjendis som rekognition finner. 

Dette kan du prøve hvis du vil ved å laste opp et png eller jpg bildet til bøtten "kandidat2006" som er av en kjendis. For så å kalle på endepunktet.

Jeg synes disse tre metrikkene gir en god oversikt over hva appen holder på med.

Timerene viser teknisk data, gaugen og meterene viser derimote data viktig for bedriften min.

## B.
Alarmen funker som den skal, fått mail. Endret thresholdet etter første alarm, så jeg ikke skulle fortsette å få mail.

![Skjermbilde av alarm mailen](/img/alarm.png)

Alle verdiene er mulig å tilpasse når du bruker modulen, men den har fornuftige defaultverdier på alt som ikke er nødvendig å endre.

Jeg valgte å ha alarm på antall bilder i bøtta fordi jeg ser at ppe bruker lang tid, og da kan det være greit å kutte ned på bilder når det når et visst punkt.
Kunne like gjerne ha hatt alarm på tiden ppe funksjonen bruker, men valgte antall bilder.

## Oppgave 5 
## A.
Kontinuerlig integrasjon er praksisen innen DevOps hvor jeg som utvikler regelmessig laster opp kode til et sentralt sted. (for eksempel github).

Her bygges koden og tester blir kjørt.

De viktigste målene med kontinuerlig integrasjon er å finne og håndtere feil raskere, forbedre programvarekvaliteten, og redusere tiden det tar å validere og slippe nye programvareoppdateringer.

Fordelene med kontinuerlig integrasjon altså å ofte validere at det fungerer som det skal er at du finner og håndterer feil raskere. Dermed vil du få en bedre kvalitet på programmet ditt og resudere tiden det tar mellom hver oppdatering.

Det finnes flere tilnærminger til hvordan man jobber i teams når det gjelder CI. Hvis man er et lite team som i dette tilfellet, kan man jobbe på samme repository.

En del viktige ting man må være obs på:
 - Sette opp branch protection, spesielt på main, men kan også være greit å beskytte de branchene branchene med satte regler. Slik at kun de endringene som man vil ha med kommer seg til main, som ofte er ditt endelig produkt.
 - Skal du lage en ny feature lager du en branch til denne.

Når man har skrevet sin feature ferdig så kan man lage en pull request til main hvor de andre i teamet kan se over, requeste endringer og godkjenne.

Her kan man også sette opp regler for hvor mange som trenger å godkjenne.
 - Sette opp github actions som bygger og tester koden.
 - Hvis man skal deploye koden så kun deploye main. Dette gjøres også med actions.
 - Du kan også lage actions som tester at koden som blir merget til main via en pullrequest får grønt lys på tester.
 - Sette opp varslinger. GitHub har som standard at når en action feiler får du mail, men du kan også utvide disse varslene til å scanne repoet for ting som ikke skal være med for eksempel aws keys.
 - Dokumentasjon av koden er viktig når man jobber alene, men et must når man jobber i teams.

Det kan også være lurt å ha dokumentasjon av de forskjellige CI prosessene i et dokument.

## B.

1. Metodikken i Scrum
- Jobber i sprinter på mellom 1 og 4 uker.
- Planlegger først, deretter koder, så review.
- Fokus på iterative utvikling.
- Har et forhåndsbestemt antall features man vil få gjort i sprinten, aldri mer, men noen ganger mindre. Da legges de i backloggen til neste sprint.
- Som det står i Scrum guiden: ["No changes are made that would endanger the Sprint Goal"](https://scrumguides.org/scrum-guide.html)
- Ser hele tiden fremover til neste feature, lite tid til forbedring, med mindre det er fokuset for sprinten.
- Fokuserer for det meste på utviklingsdelen av programmets livssyklus. 
- Kan være mer nyttig hvis kravene fra kunden ofte endres.

Med denne metodikken vil man ikke ha like stor mulighet til kontinuerlig forbedring, siden man da kanskje kun vil integrere featurene i slutten av sprinten. Hvis den får grøt lys integreres den og man er ferdig med den.

Det er daglige møter (daily standups) som kan ta mye tid, hvis scrum masteren ikke er effektiv.

Hvis det er et stort prosjekt med flere scrum teams, krever det god kommunikasjon mellom dem.

Når man skal i gang med en sprint krever det at utviklerne er gode til å estimere hvor lang tid ting tar for at man skal komme i mål med sprint målet.

2. Metodikken i DevOps
- Fokuserer på integrasjon og leveranse av koden samt overvåkning av kode i produksjon.
- Tre hovedrprinsipper:
    - flyt - automatisere pipelinen din
    - feedback - rask tilbakemelding på om den funker som den skal eller om det har oppstått et problem
    - kontinuerlig forbedring - se den overnevnte feedbacken og fikse problemer.
- Man gjør små endringer hele tiden, hvis det oppstår feil er det en del av læringen. 
- Oppfordrer til ekperimenteringer siden man inkrementerer koden i små biter er det lett å hoppe tilbake til et tidligere punkt i koden.
- Mest effektiv ved veldig konkrete mål.

Siden man har satt opp pipelinen sin til å gi deg feedback hvis noe feiler, gir dette et solid grunnlag for et godt produkt.
Det er essensielt at man da har satt opp alt dette rundt, gode tester, gode actions på for eksempel github, gode varsler.
Siden DevOps oppfordrer til hyppige deployments vil dette øke leveransetempoet.
På grunn av de hyppige deploymentsene kan det hende man noen ganger nedprioriterer sikkerhet. I dagens teknologiske samfunn er det en stor ulempe.

3. Sammenligning
Jeg tenker man ikke kan sammenligne disse to metodikkene 1 til 1. Etter slik jeg har forstått det prøver de ikke å løse de samme problemene.
Scrum er en utviklingsprosess og sier ingen ting om det som skjer etter du har levert koden eller feedback, utenom i retrospektive, men det handler mer om prosessen enn feedback til koden.
DevOps derimot sier ingenting om gruppestrukturer, ledere, produkteiere og heller ingenting om tidsrammer utenom at ting skal skje daglig.
Siden sprinter i scrum kan vare alt fra en til fire uker, vil du kunne raskere levere et produkt med DevOps.
Jeg ser ingenting i veien for at man skal kunne implementere konsepter fra devops inn i en sprint. Ved at du har automatiserte pipelines som gir deg feedback som du da forbedrer.
Jeg vil ikke si man må velge en, men at de komplimenterer hverandre mer enn at de er motsetninger.

## C.
I dette tenkte scenarioet har jeg laget en tinder-feature for linkedIn hvor en bruker kan stille inn om de er arbeidstakere eller arbeidssøkere.
Deretter er det bare å swipe i vei, arbeidssøkere vil få opp stillinger som matcher deres profil, og arbeidstakere vil få opp søkere som matcher deres stilling.
 1. Sette opp tester i koden min.
 2. Lage github action som builder og tester på min branch (ikke main). Dette er første instans av feedback.
 3. Micrometer til å sette inn målepunkter i koden som måler diverse metrics. Et par eksempler:
    - Antall brukere inne på tjenesten.
    - Antall swipes til høyre siste 5 min
    - Antall swipes til venstre siste 5 min
 4. Deretter kan jeg bruke terraform til å lage et dashboard som viser disse metrikkene. I for eksemepl cloudwatch.
 5. Her kan jeg se hvordan det går med featuren min og kan sette opp alarmer med cloudwatch som bruker SNS til å sende meg mail, hvis det siste timen ikke har vært noen brukere for eksempel.
 5. Jeg setter også opp alarmer som aktiveres når jeg får x antall 500 feilmeldinger fra endepunktene.
 6. Dette kan signalisere at det er noe galt.

Hvis man har laget noe så kan man fort bli blind på eget arbeid. Feedback er derfor essensielt.
Man har mange former for feedback
 - in person feedback, hvis man par programmerer kan det komme øyeblikkelig.
 - har man tester kan det komme etter man kjører dem.
 - har man github actions får man mail før det kommer ut i produksjon hvis den feiler.
 - har man alarmer og metrikker får man feedback i produksjon.
Alle disse formene for feedback gjør det lettere å lage gode produkter.

Mellom hvert steg er det nye forbedringer, og så en pil opp. Blir det godkjent der går man videre til neste steg.
Dette øker kodekvaliteten, ved å hele tiden gjøre små endringer og få tilbakemelding på disse endringene.
 
