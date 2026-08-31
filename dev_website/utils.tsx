import React from 'react';
import { FaGithub, FaTwitter, FaFacebook, FaLinkedin, FaInstagram } from 'react-icons/fa';

export const getPlatformIconByName = (name: string): React.ReactNode => {
    switch (name.toLowerCase()) {
        case 'github':
            return <FaGithub size={24} />;
        case 'twitter':
        case 'x':
            return <FaTwitter size={24} />;
        case 'facebook':
            return <FaFacebook size={24} />;
        case 'linkedin':
            return <FaLinkedin size={24} />;
        case 'instagram':
            return <FaInstagram size={24} />;
        default:
            return null;
    }
};
