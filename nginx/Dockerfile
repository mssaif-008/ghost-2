FROM nginx:1.25-alpine

RUN rm /etc/nginx/conf.d/default.conf

COPY default.conf.template /etc/nginx/templates/default.conf.template

RUN mkdir -p /etc/nginx/certs

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]