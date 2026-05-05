import os
import uuid
import random
from datetime import datetime, timedelta

XML_HEADER = """<?xml version="1.0" encoding="UTF-8"?>
<!-- This is FICTITIOUS TEST DATA generated for the SWIM Developer project.
     Data model based on EUROCAE ED-254 (Arrival Sequence Service Performance Standard)
     and FIXM 4.3 (Flight Information Exchange Model).
     All validation credits belong to their respective owners (EUROCAE, FIXM CCB, EUROCONTROL).

     THIS FICTITIOUS DATA SET IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
     INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
     FOR A PARTICULAR PURPOSE ARE DISCLAIMED.

     Created by Marcelo Sales (marcelo.d.sales@gmail.com) for the SWIM Developer project.
     Project: https://github.com/orgs/swim-developer/repositories
     Purpose: Testing and validation of SWIM Extended AMAN (E-AMAN) services.
     All credits to the original specification authors must be preserved. -->"""

AIRPORTS = [
    "LPPT", "LPPR", "LEMD", "LEBL", "LEPA", "LFPG", "LFMN", "LFLL",
    "EDDF", "EDDM", "EDDB", "ESSA", "EGLL", "EGKK", "LIRF", "LIMC",
    "EHAM", "EBBR", "LSZH", "LOWW", "EIDW", "ENGM", "EKCH", "EDDL"
]

AIRLINES = [
    "TAP", "IBE", "AFR", "DLH", "SAS", "BAW", "KLM", "BEL", "SWR",
    "AUA", "EIN", "NAX", "RYR", "EJU", "VLG", "THY", "LOT", "TRA"
]

WAYPOINTS = [
    "AMRAM", "BOMBI", "KONAN", "SPESA", "SUGOL", "PINAR", "TOUKA",
    "MALOT", "GAVOS", "LURAC", "SITET", "UNOKO", "ODINA", "NUNOS"
]

RUNWAYS = [
    "01L", "01R", "03", "07L", "07R", "09", "18C", "18R",
    "25L", "25R", "27L", "27R", "32L", "32R"
]

STATUSES = [
    "SEQUENCED_STABLE", "SEQUENCED_UNSTABLE", "MANUAL_INTERVENTION",
    "SEQUENCED_FROZEN", "TIME_FROZEN", "IS_ON_FINAL", "HAS_LANDED", "DESEQUENCED"
]

PLANNING_STATUSES = ["ESTIMATED_TIME", "TARGET_TIME", "CONTROLLED_TIME"]

POINT_USAGES = [
    "INITIAL_METERING_FIX", "METERING_FIX", "INITIAL_APPROACH_FIX",
    "FINAL_APPROACH_FIX", "STACK_EXIT", "COORDINATION"
]

WAKE_TURBULENCE = ["L", "M", "H", "J"]
AIRCRAFT_TYPES = ["A319", "A320", "A321", "A388", "B738", "B772", "B789", "E190", "E195", "CRJ9"]
EXCEPTIONS = ["AMAN_UNAVAILABLE", "AMAN_DEGRADED", "SEQUENCING_DISABLED"]


def fmt_time(dt):
    return dt.strftime("%Y-%m-%dT%H:%M:%SZ")


def fmt_time_ms(dt):
    return dt.strftime("%Y-%m-%dT%H:%M:%S.") + f"{dt.microsecond // 1000:03d}Z"


def generate_entry(base_time, seq_num, destination, is_last):
    adep = random.choice([a for a in AIRPORTS if a != destination])
    airline = random.choice(AIRLINES)
    arcid = f"{airline}{random.randint(10, 9999)}"
    landing_offset = 20 + seq_num * random.randint(3, 6)
    landing_time = base_time + timedelta(minutes=landing_offset)
    status = random.choice(STATUSES)
    runway = random.choice(RUNWAYS)

    entry_lines = [
        "        <arrivalManagementInformation>",
        f"            <amanTargetLandingTime>{fmt_time(landing_time)}</amanTargetLandingTime>",
        f"            <arrivalManagementHandlingIndicator>{status}</arrivalManagementHandlingIndicator>",
        f"            <lastFiledRecord>{'true' if is_last else 'false'}</lastFiledRecord>",
        f"            <sequenceNumber>{seq_num}</sequenceNumber>",
        f"            <typeOfAircraft>{random.choice(AIRCRAFT_TYPES)}</typeOfAircraft>",
        f"            <wakeTurbulenceIcaoCategory>{random.choice(WAKE_TURBULENCE)}</wakeTurbulenceIcaoCategory>",
        f"            <assignedArrivalRunway>{runway}</assignedArrivalRunway>",
    ]

    if random.random() > 0.2:
        fix_time = landing_time - timedelta(minutes=random.randint(8, 25))
        waypoint = random.choice(WAYPOINTS)
        point_usage = random.choice(POINT_USAGES)
        planning = random.choice(PLANNING_STATUSES)

        entry_lines.append("            <meteringInformation>")
        entry_lines.append(f"                <amanTargetTimeOver>{fmt_time(fix_time)}</amanTargetTimeOver>")

        if random.random() > 0.4:
            delay_secs = random.randint(30, 600)
            mins = delay_secs // 60
            secs = delay_secs % 60
            dur = f"PT{mins}M{secs}S" if secs else f"PT{mins}M"
            entry_lines.append(f"                <delayAtPoint>{dur}</delayAtPoint>")

        entry_lines.append(f"                <planningStatus>{planning}</planningStatus>")
        entry_lines.append(f"                <pointName>{waypoint}</pointName>")
        entry_lines.append(f"                <pointUsage>{point_usage}</pointUsage>")

        if random.random() > 0.5:
            adv_lines = []
            if random.random() > 0.5:
                adv_lines.append(f"                    <routeAdvisory>DIRECT {random.choice(WAYPOINTS)}</routeAdvisory>")
            if random.random() > 0.3:
                adv_lines.append(f"                    <speedAdvisory>MACH 0.{random.randint(68, 84)}</speedAdvisory>")
            if random.random() > 0.5:
                adv_lines.append(f"                    <timeToGainOrLose>{random.randint(-300, 300)}</timeToGainOrLose>")
            if adv_lines:
                entry_lines.append("                <advisoryInformation>")
                entry_lines.extend(adv_lines)
                entry_lines.append("                </advisoryInformation>")

        entry_lines.append("            </meteringInformation>")

    entry_lines.append("            <flightIdentification>")
    entry_lines.append(f"                <arcid>{arcid}</arcid>")
    entry_lines.append(f"                <ades>{destination}</ades>")
    entry_lines.append(f"                <adep>{adep}</adep>")
    gufi_val = str(uuid.uuid4())
    gufi_time = fmt_time(base_time - timedelta(hours=random.randint(1, 12)))
    ns_domain = random.choice(["LOCATION_INDICATOR", "FULLY_QUALIFIED_DOMAIN_NAME"])
    ns_id = adep if ns_domain == "LOCATION_INDICATOR" else "provider.swim.aero"
    entry_lines.append(f'                <gufi codeSpace="urn:uuid"')
    entry_lines.append(f'                      creationTime="{gufi_time}"')
    entry_lines.append(f'                      namespaceDomain="{ns_domain}"')
    entry_lines.append(f'                      namespaceIdentifier="{ns_id}">{gufi_val}</gufi>')
    entry_lines.append("            </flightIdentification>")

    if random.random() > 0.8:
        delay_total = random.randint(60, 600)
        entry_lines.append("            <totalDelay>")
        entry_lines.append(f"                <arrivalDelay>{delay_total}</arrivalDelay>")
        entry_lines.append("            </totalDelay>")

    entry_lines.append("        </arrivalManagementInformation>")
    return "\n".join(entry_lines)


def generate_arrival_sequence(file_id):
    destination = random.choice(AIRPORTS)
    now = datetime(2026, 4, 8, random.randint(6, 22), random.randint(0, 59), 0)
    creation = now
    publication = now + timedelta(seconds=1)
    outage = random.random() < 0.1
    num_entries = random.randint(2, 8)

    entries = []
    for i in range(1, num_entries + 1):
        entries.append(generate_entry(now, i, destination, i == num_entries))

    xml = f"""{XML_HEADER}
<arrivalSequence xmlns="http://www.fixm.aero/ed254/1.0"
                 xmlns:fb="http://www.fixm.aero/base/4.3"
                 xmlns:fx="http://www.fixm.aero/flight/4.3">
    <creationTime>{fmt_time(creation)}</creationTime>
    <publicationTime>{fmt_time_ms(publication)}</publicationTime>
    <firstMessageAfterServiceOutage>{'true' if outage else 'false'}</firstMessageAfterServiceOutage>
    <aerodromeDesignator>{destination}</aerodromeDesignator>
    <sequenceEntries>
{chr(10).join(entries)}
    </sequenceEntries>
</arrivalSequence>
"""
    return destination, xml.strip() + "\n"


def generate_provider_exception():
    exc = random.choice(EXCEPTIONS)
    xml = f"""{XML_HEADER}
<providerExceptions xmlns="http://www.fixm.aero/ed254/1.0">
    <provException>{exc}</provException>
</providerExceptions>
"""
    return exc, xml.strip() + "\n"


if __name__ == "__main__":
    output_folder = "ed254_samples"
    os.makedirs(output_folder, exist_ok=True)

    seq_count = 130
    exc_count = 20

    print(f"Generating {seq_count} arrivalSequence + {exc_count} providerExceptions samples...")

    for i in range(1, seq_count + 1):
        dest, xml = generate_arrival_sequence(i)
        path = os.path.join(output_folder, f"SEQ_{dest}_{i:03d}.xml")
        with open(path, "w", encoding="utf-8") as f:
            f.write(xml)

    for i in range(1, exc_count + 1):
        exc, xml = generate_provider_exception()
        path = os.path.join(output_folder, f"EXC_{exc}_{i:03d}.xml")
        with open(path, "w", encoding="utf-8") as f:
            f.write(xml)

    print(f"Done! {seq_count + exc_count} XSD-valid files in '{output_folder}'.")
