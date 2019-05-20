FROM adoptopenjdk/openjdk11:latest
VOLUME /avdpool_data
EXPOSE 8888

WORKDIR /home/avdpool

RUN useradd -ms /bin/bash avdpool

ARG DEPENDENCY=build/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /home/avdpool/app/lib
COPY ${DEPENDENCY}/META-INF /home/avdpool/app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /home/avdpool/app
RUN chown -R avdpool /home/avdpool/app

USER avdpool

ENTRYPOINT ["java","-cp","/home/avdpool/app:/home/avdpool/app/lib/*","ch.so.agi.avdpool.AvdpoolApplication"]
