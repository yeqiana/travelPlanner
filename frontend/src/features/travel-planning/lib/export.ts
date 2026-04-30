import { Itinerary } from '../../../shared/types/travel';

export function exportItineraryToICS(itinerary: Itinerary) {
  const today = new Date();
  const startDate = new Date(today);
  startDate.setDate(startDate.getDate() + 1); // Start tomorrow

  const icsContent = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//AI Travel Agent//ZH',
    'CALSCALE:GREGORIAN',
    'METHOD:PUBLISH'
  ];

  itinerary.days.forEach((day, dayIndex) => {
    const currentDay = new Date(startDate);
    currentDay.setDate(currentDay.getDate() + day.dayNumber - 1);
    
    day.activities.forEach((act, actIndex) => {
      let hours = 9;
      let minutes = 0;
      const timeMatch = act.time?.match(/(\d{1,2}):(\d{2})/);
      if (timeMatch) {
        hours = parseInt(timeMatch[1], 10);
        minutes = parseInt(timeMatch[2], 10);
      }

      const startDateTime = new Date(currentDay);
      startDateTime.setHours(hours, minutes, 0);

      let durationHours = 2;
      let durationMinutes = 0;
      const durHourMatch = act.duration?.match(/(\d+(?:\.\d+)?)\s*小时/);
      if (durHourMatch) {
        durationHours = parseFloat(durHourMatch[1]);
      } else {
        const durMinMatch = act.duration?.match(/(\d+)\s*分钟/);
        if (durMinMatch) {
           durationHours = 0;
           durationMinutes = parseInt(durMinMatch[1], 10);
        }
      }

      const endDateTime = new Date(startDateTime);
      endDateTime.setHours(startDateTime.getHours() + Math.floor(durationHours));
      endDateTime.setMinutes(startDateTime.getMinutes() + (durationHours % 1) * 60 + durationMinutes);

      const dToStr = (d: Date) => {
        return d.getUTCFullYear() +
          String(d.getUTCMonth() + 1).padStart(2, '0') +
          String(d.getUTCDate()).padStart(2, '0') + 'T' +
          String(d.getUTCHours()).padStart(2, '0') +
          String(d.getUTCMinutes()).padStart(2, '0') +
          String(d.getUTCSeconds()).padStart(2, '0') + 'Z';
      };

      const uid = `${Date.now()}-${dayIndex}-${actIndex}@aitravelagent.app`;

      icsContent.push('BEGIN:VEVENT');
      icsContent.push(`UID:${uid}`);
      icsContent.push(`DTSTAMP:${dToStr(new Date())}`);
      icsContent.push(`DTSTART:${dToStr(startDateTime)}`);
      icsContent.push(`DTEND:${dToStr(endDateTime)}`);
      icsContent.push(`SUMMARY:${act.location}`);
      icsContent.push(`DESCRIPTION:${act.description.replace(/\n/g, '\\n')}\\n\\n预计时长: ${act.duration}${act.transportationToNext ? '\\n交通: ' + act.transportationToNext : ''}`);
      icsContent.push(`LOCATION:${act.location}`);
      icsContent.push('END:VEVENT');
    });
  });

  icsContent.push('END:VCALENDAR');

  const blob = new Blob([icsContent.join('\r\n')], { type: 'text/calendar;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${itinerary.title}.ics`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
